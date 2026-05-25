package com.zapbot.android.whatsapp

import com.zapbot.android.domain.IncomingWhatsAppMessage
import com.zapbot.android.domain.WhatsAppConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class FakeWhatsAppClient : WhatsAppClient {
    private val state = MutableStateFlow<WhatsAppConnectionState>(WhatsAppConnectionState.Disconnected)
    private val savedSession = MutableStateFlow(false)
    private val messages = MutableSharedFlow<IncomingWhatsAppMessage>(extraBufferCapacity = 32)
    val sentTexts = mutableListOf<String>()
    val sentMedia = mutableListOf<File>()

    override val connectionState: Flow<WhatsAppConnectionState> = state
    override val hasSavedSession: Flow<Boolean> = savedSession
    override val incomingMessages: Flow<IncomingWhatsAppMessage> = messages

    override suspend fun start() {
        state.value = WhatsAppConnectionState.Running
    }

    override suspend fun stop() {
        state.value = WhatsAppConnectionState.Disconnected
    }

    override suspend fun clearSession() {
        sentTexts.clear()
        sentMedia.clear()
        savedSession.value = false
    }

    override suspend fun requestPairingCode(phoneNumberE164: String): String {
        state.value = WhatsAppConnectionState.WaitingForQr("FAKE-QR-$phoneNumberE164")
        savedSession.value = true
        return "123-456"
    }

    override suspend fun sendText(chatId: String, text: String, replyToMessageId: String?): String {
        sentTexts += "$chatId:$text"
        return "fake-sent-${sentTexts.size}"
    }

    override suspend fun sendTextToGroupName(groupName: String, text: String): String {
        sentTexts += "$groupName:$text"
        return "fake-group-${sentTexts.size}"
    }

    override suspend fun sendMedia(chatId: String, file: File, caption: String?, replyToMessageId: String?) {
        sentMedia += file
    }

    suspend fun receive(text: String, chatId: String = "chat-1", id: String = System.nanoTime().toString()) {
        messages.emit(
            IncomingWhatsAppMessage(
                id = id,
                chatId = chatId,
                senderName = "Teste",
                text = text,
                quotedMessageId = null,
                quotedText = null,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
