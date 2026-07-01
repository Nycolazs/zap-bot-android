package com.zapbot.android.domain

object WhatsAppChatPolicy {
    fun isPrivateChat(chatId: String): Boolean {
        val jid = normalizedJid(chatId)
        if (jid.isBlank()) return false
        if (isGroupChat(jid) || isStatusChat(jid) || isBroadcastChat(jid) || isNewsletterChat(jid)) return false
        return jid.endsWith("@s.whatsapp.net", ignoreCase = true) ||
            jid.endsWith("@c.us", ignoreCase = true) ||
            jid.endsWith("@lid", ignoreCase = true)
    }

    fun isGroupChat(chatId: String): Boolean =
        normalizedJid(chatId).endsWith("@g.us", ignoreCase = true)

    fun isStatusChat(chatId: String): Boolean =
        normalizedJid(chatId).equals("status@broadcast", ignoreCase = true)

    fun isBroadcastChat(chatId: String): Boolean =
        normalizedJid(chatId).endsWith("@broadcast", ignoreCase = true)

    fun isNewsletterChat(chatId: String): Boolean =
        normalizedJid(chatId).endsWith("@newsletter", ignoreCase = true)

    private fun normalizedJid(chatId: String): String =
        chatId.trim().let { value ->
            if (value.substringBefore(':').endsWith("@g.us", ignoreCase = true)) {
                value.substringBefore(':')
            } else {
                value
            }
        }
}

fun IncomingWhatsAppMessage.isPrivateChat(): Boolean =
    WhatsAppChatPolicy.isPrivateChat(chatId)
