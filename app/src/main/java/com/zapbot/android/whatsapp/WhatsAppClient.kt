package com.zapbot.android.whatsapp

import com.zapbot.android.domain.IncomingWhatsAppMessage
import com.zapbot.android.domain.WhatsAppConnectionState
import kotlinx.coroutines.flow.Flow
import java.io.File

interface WhatsAppClient {
    val connectionState: Flow<WhatsAppConnectionState>
    val hasSavedSession: Flow<Boolean>
    val incomingMessages: Flow<IncomingWhatsAppMessage>

    suspend fun start()
    suspend fun stop()
    suspend fun clearSession()
    suspend fun requestPairingCode(phoneNumberE164: String): String
    suspend fun sendText(chatId: String, text: String, replyToMessageId: String? = null): String?
    suspend fun sendTextToGroupName(groupName: String, text: String): String?
    suspend fun sendMedia(chatId: String, file: File, caption: String? = null, replyToMessageId: String? = null)
}
