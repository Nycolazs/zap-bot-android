package com.zapbot.android.whatsapp

import com.squareup.moshi.Moshi
import com.zapbot.android.domain.IncomingMediaType
import com.zapbot.android.domain.IncomingWhatsAppMedia
import com.zapbot.android.domain.IncomingWhatsAppMessage
import com.zapbot.android.domain.WhatsAppConnectionState
import com.zapbot.android.domain.WhatsAppChatPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.File

class BridgeWhatsAppClient(
    bridgeUrl: String,
    moshi: Moshi
) : WhatsAppClient {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val state = MutableStateFlow<WhatsAppConnectionState>(WhatsAppConnectionState.Disconnected)
    private val savedSession = MutableStateFlow(false)
    private val messages = MutableSharedFlow<IncomingWhatsAppMessage>(extraBufferCapacity = 64)
    private val api: BridgeApi
    private var polling: Job? = null
    private var lastSeq = 0L

    override val connectionState: Flow<WhatsAppConnectionState> = state
    override val hasSavedSession: Flow<Boolean> = savedSession
    override val incomingMessages: Flow<IncomingWhatsAppMessage> = messages

    init {
        val normalizedUrl = if (bridgeUrl.endsWith("/")) bridgeUrl else "$bridgeUrl/"
        api = Retrofit.Builder()
            .baseUrl(normalizedUrl)
            .client(OkHttpClient.Builder().retryOnConnectionFailure(true).build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BridgeApi::class.java)
    }

    override suspend fun start() {
        state.value = WhatsAppConnectionState.Connecting
        runCatching { api.start() }
            .onFailure {
                state.value = WhatsAppConnectionState.Error("Bridge offline: ${safeError(it)}", it)
                return
            }
        polling?.cancel()
        polling = scope.launch {
            while (isActive) {
                runCatching {
                    val current = api.state()
                    state.value = current.toConnectionState()
                    savedSession.value = current.hasSession()
                    val batch = api.messages(lastSeq)
                    batch.messages.forEach {
                        lastSeq = maxOf(lastSeq, it.seq)
                        if (it.text.isNotBlank() && WhatsAppChatPolicy.isPrivateChat(it.chatId)) {
                            messages.emit(
                                IncomingWhatsAppMessage(
                                    id = it.id,
                                    chatId = it.chatId,
                                    senderName = it.senderName,
                                    text = it.text,
                                    quotedMessageId = null,
                                    quotedText = null,
                                    timestamp = it.timestamp,
                                    media = it.toMedia(),
                                    quotedMedia = it.toQuotedMedia()
                                )
                            )
                        }
                    }
                }.onFailure {
                    state.value = WhatsAppConnectionState.Error("Bridge failure: ${safeError(it)}", it)
                }
                delay(2_000)
            }
        }
    }

    override suspend fun stop() {
        polling?.cancel()
        runCatching { api.stop() }
        state.value = WhatsAppConnectionState.Disconnected
    }

    override suspend fun clearSession() {
        api.clearSession()
        lastSeq = 0
        savedSession.value = false
    }

    override suspend fun requestPairingCode(phoneNumberE164: String): String {
        val response = api.pairingCode(PairingCodeRequest(phoneNumberE164))
        state.value = response.state.toConnectionState()
        savedSession.value = response.state.hasSession()
        return response.code
    }

    override suspend fun sendText(chatId: String, text: String, replyToMessageId: String?): String? {
        if (!WhatsAppChatPolicy.isPrivateChat(chatId)) return null
        api.sendText(SendTextRequest(chatId, text, replyToMessageId))
        return null
    }

    override suspend fun sendTextToGroupName(groupName: String, text: String): String? = null

    override suspend fun sendMedia(chatId: String, file: File, caption: String?, replyToMessageId: String?) {
        error("Envio de media via bridge ainda precisa de upload multipart")
    }

    override suspend fun sendSticker(chatId: String, image: File, replyToMessageId: String?) {
        error("Envio de figurinha via bridge HTTP ainda precisa de upload multipart")
    }

    private fun BridgeStateResponse.toConnectionState(): WhatsAppConnectionState = when (status) {
        "waiting_qr" -> WhatsAppConnectionState.WaitingForQr(qrData.orEmpty())
        "connecting" -> WhatsAppConnectionState.Connecting
        "connected" -> WhatsAppConnectionState.Connected(phoneNumber)
        "running" -> WhatsAppConnectionState.Running
        "error" -> WhatsAppConnectionState.Error(error ?: "Bridge error")
        else -> WhatsAppConnectionState.Disconnected
    }

    private fun BridgeStateResponse.hasSession(): Boolean =
        status == "connected" || status == "running" || !phoneNumber.isNullOrBlank()

    private fun safeError(t: Throwable): String =
        (t.message ?: t.javaClass.simpleName).replace(Regex("[\\r\\n]+"), " ").take(160)
}

private fun BridgeMessage.toMedia(): IncomingWhatsAppMedia? {
    val path = mediaPath?.takeIf { it.isNotBlank() } ?: return null
    val type = when (mediaType) {
        "image", null, "" -> IncomingMediaType.IMAGE
        else -> return null
    }
    return IncomingWhatsAppMedia(
        type = type,
        file = File(path),
        mimeType = mediaMime?.takeIf { it.isNotBlank() } ?: "application/octet-stream",
        fileName = mediaFileName
    )
}

private fun BridgeMessage.toQuotedMedia(): IncomingWhatsAppMedia? {
    val path = quotedMediaPath?.takeIf { it.isNotBlank() } ?: return null
    val type = when (quotedMediaType?.lowercase()) {
        null, "", "image" -> IncomingMediaType.IMAGE
        else -> return null
    }
    return IncomingWhatsAppMedia(
        type = type,
        file = File(path),
        mimeType = quotedMediaMime?.takeIf { it.isNotBlank() } ?: "application/octet-stream",
        fileName = quotedMediaFileName
    )
}

private interface BridgeApi {
    @POST("start")
    suspend fun start(): BridgeStateResponse

    @POST("stop")
    suspend fun stop(): BridgeStateResponse

    @POST("clear-session")
    suspend fun clearSession(): BridgeStateResponse

    @POST("pairing-code")
    suspend fun pairingCode(@Body body: PairingCodeRequest): PairingCodeResponse

    @POST("send-text")
    suspend fun sendText(@Body body: SendTextRequest)

    @GET("state")
    suspend fun state(): BridgeStateResponse

    @GET("messages")
    suspend fun messages(@Query("after") after: Long): MessagesResponse
}

private data class PairingCodeRequest(val phoneNumber: String)
private data class PairingCodeResponse(val code: String, val state: BridgeStateResponse)
private data class SendTextRequest(val chatId: String, val text: String, val replyToMessageId: String?)
private data class BridgeStateResponse(
    val status: String,
    val qrData: String? = null,
    val phoneNumber: String? = null,
    val error: String? = null
)
private data class MessagesResponse(val messages: List<BridgeMessage> = emptyList())
private data class BridgeMessage(
    val seq: Long,
    val id: String,
    val chatId: String,
    val senderName: String?,
    val text: String,
    val timestamp: Long,
    val mediaType: String? = null,
    val mediaPath: String? = null,
    val mediaMime: String? = null,
    val mediaFileName: String? = null,
    val quotedMediaType: String? = null,
    val quotedMediaPath: String? = null,
    val quotedMediaMime: String? = null,
    val quotedMediaFileName: String? = null
)
