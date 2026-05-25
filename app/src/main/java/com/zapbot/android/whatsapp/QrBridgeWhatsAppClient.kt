package com.zapbot.android.whatsapp

import com.zapbot.android.domain.IncomingWhatsAppMessage
import com.zapbot.android.domain.WhatsAppConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File

class QrBridgeWhatsAppClient : WhatsAppClient {
    private val state = MutableStateFlow<WhatsAppConnectionState>(WhatsAppConnectionState.Disconnected)
    private val savedSession = MutableStateFlow(false)
    override val connectionState: Flow<WhatsAppConnectionState> = state
    override val hasSavedSession: Flow<Boolean> = savedSession
    override val incomingMessages: Flow<IncomingWhatsAppMessage> = MutableSharedFlow<IncomingWhatsAppMessage>()

    override suspend fun start() {
        state.value = WhatsAppConnectionState.Error(
            "WhatsApp QR bridge is not configured. Integrate a local companion or use Cloud API with a webhook."
        )
    }

    override suspend fun stop() {
        state.value = WhatsAppConnectionState.Disconnected
    }

    override suspend fun clearSession() = Unit

    override suspend fun requestPairingCode(phoneNumberE164: String): String {
        state.value = WhatsAppConnectionState.Error(
            "WhatsApp QR bridge is not configured. The recommended path is whatsmeow as a linked device behind this adapter."
        )
        error("WhatsApp QR bridge is not configured")
    }

    override suspend fun sendText(chatId: String, text: String, replyToMessageId: String?): String? {
        error("WhatsApp QR bridge is not configured")
    }

    override suspend fun sendTextToGroupName(groupName: String, text: String): String? {
        error("WhatsApp QR bridge is not configured")
    }

    override suspend fun sendMedia(chatId: String, file: File, caption: String?, replyToMessageId: String?) {
        error("WhatsApp QR bridge is not configured")
    }
}
