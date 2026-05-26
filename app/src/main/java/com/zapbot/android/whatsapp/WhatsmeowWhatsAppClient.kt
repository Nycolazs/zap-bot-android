package com.zapbot.android.whatsapp

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import com.zapbot.android.domain.IncomingWhatsAppMessage
import com.zapbot.android.domain.WhatsAppConnectionState
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import whatsmeowbridge.Bridge
import whatsmeowbridge.Listener
import whatsmeowbridge.Whatsmeowbridge
import java.io.File
import java.util.concurrent.Executors

class WhatsmeowWhatsAppClient(app: Application) : WhatsAppClient {
    private val bridgeDispatcher = Executors.newFixedThreadPool(4) { runnable ->
        Thread(runnable, "zapbot-whatsapp").apply {
            priority = Thread.MAX_PRIORITY
        }
    }.asCoroutineDispatcher()
    private val storeDir = File(app.filesDir, "whatsmeow")
    private val storeDb = File(storeDir, "whatsmeow.db")
    private val state = MutableStateFlow<WhatsAppConnectionState>(WhatsAppConnectionState.Disconnected)
    private val savedSession = MutableStateFlow(readSavedSession())
    private val messages = MutableSharedFlow<IncomingWhatsAppMessage>(extraBufferCapacity = 128)
    private val bridge: Bridge = Whatsmeowbridge.newBridge(
        storeDir.absolutePath,
        object : Listener {
            override fun onState(state: String, detail: String) {
                this@WhatsmeowWhatsAppClient.state.value = state.toConnectionState(detail)
                refreshSavedSession()
            }

            override fun onPairingCode(code: String) {
                lastPairingCode.value = code
            }

            override fun onMessage(id: String, chatID: String, senderName: String, text: String, quotedMessageID: String, quotedText: String, timestampMillis: Long) {
                messages.tryEmit(
                    IncomingWhatsAppMessage(
                        id = id,
                        chatId = chatID,
                        senderName = senderName.ifBlank { null },
                        text = text,
                        quotedMessageId = quotedMessageID.ifBlank { null },
                        quotedText = quotedText.ifBlank { null },
                        timestamp = timestampMillis
                    )
                )
            }

            override fun onError(message: String) {
                this@WhatsmeowWhatsAppClient.state.value = WhatsAppConnectionState.Error(message)
                refreshSavedSession()
            }
        }
    )
    private val lastPairingCode = MutableStateFlow<String?>(null)

    override val connectionState: Flow<WhatsAppConnectionState> = state
    override val hasSavedSession: Flow<Boolean> = savedSession
    override val incomingMessages: Flow<IncomingWhatsAppMessage> = messages

    override suspend fun start() = withContext(bridgeDispatcher) {
        refreshSavedSession()
        state.value = WhatsAppConnectionState.Connecting
        bridge.start()
        refreshSavedSession()
    }

    override suspend fun stop() = withContext(bridgeDispatcher) {
        bridge.stop()
        state.value = WhatsAppConnectionState.Disconnected
        refreshSavedSession()
    }

    override suspend fun clearSession() = withContext(bridgeDispatcher) {
        bridge.clearSession()
        lastPairingCode.value = null
        savedSession.value = false
        state.value = WhatsAppConnectionState.Disconnected
    }

    override suspend fun requestPairingCode(phoneNumberE164: String): String = withContext(bridgeDispatcher) {
        refreshSavedSession()
        state.value = WhatsAppConnectionState.Connecting
        val code = bridge.pairPhone(phoneNumberE164)
        lastPairingCode.value = code
        refreshSavedSession()
        code
    }

    override suspend fun sendText(chatId: String, text: String, replyToMessageId: String?): String? = withContext(bridgeDispatcher) {
        bridge.sendText(chatId, text)
    }

    override suspend fun sendTextToGroupName(groupName: String, text: String): String? = withContext(bridgeDispatcher) {
        bridge.sendTextToGroupName(groupName, text)
    }

    override suspend fun sendMedia(chatId: String, file: File, caption: String?, replyToMessageId: String?) = withContext(bridgeDispatcher) {
        bridge.sendMedia(chatId, file.absolutePath, caption.orEmpty(), mimeType(file))
    }

    private fun String.toConnectionState(detail: String): WhatsAppConnectionState = when (this) {
        "connecting" -> WhatsAppConnectionState.Connecting
        "running" -> WhatsAppConnectionState.Running
        "waiting_pairing" -> WhatsAppConnectionState.WaitingForQr(detail.ifBlank { "Generate a Linked Devices pairing code in Settings." })
        "disconnected" -> WhatsAppConnectionState.Disconnected
        else -> WhatsAppConnectionState.Error(detail.ifBlank { this })
    }

    private fun mimeType(file: File): String = when (file.extension.lowercase()) {
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "m4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "ogg", "opus" -> "audio/ogg"
        "zip" -> "application/zip"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        else -> "application/octet-stream"
    }

    private fun refreshSavedSession() {
        savedSession.value = readSavedSession()
    }

    private fun readSavedSession(): Boolean {
        if (!storeDb.exists()) return false
        return runCatching {
            SQLiteDatabase.openDatabase(storeDb.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                db.rawQuery(
                    "SELECT COUNT(*) FROM whatsmeow_device WHERE jid IS NOT NULL AND jid != ''",
                    emptyArray()
                ).use { cursor ->
                    cursor.moveToFirst() && cursor.getInt(0) > 0
                }
            }
        }.getOrDefault(false)
    }
}
