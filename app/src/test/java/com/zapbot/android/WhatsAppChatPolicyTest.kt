package com.zapbot.android

import com.zapbot.android.domain.WhatsAppChatPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WhatsAppChatPolicyTest {
    @Test fun allowsOnlyPrivateContactChats() {
        assertTrue(WhatsAppChatPolicy.isPrivateChat("5511999999999@s.whatsapp.net"))
        assertTrue(WhatsAppChatPolicy.isPrivateChat("5511999999999@c.us"))
        assertTrue(WhatsAppChatPolicy.isPrivateChat("123456789@lid"))
        assertTrue(WhatsAppChatPolicy.isPrivateChat("5511999999999:42@s.whatsapp.net"))
    }

    @Test fun blocksGroupsStatusBroadcastsAndNewsletters() {
        assertFalse(WhatsAppChatPolicy.isPrivateChat("120363000000000000@g.us"))
        assertFalse(WhatsAppChatPolicy.isPrivateChat("120363000000000000@g.us:5511999999999@s.whatsapp.net"))
        assertFalse(WhatsAppChatPolicy.isPrivateChat("status@broadcast"))
        assertFalse(WhatsAppChatPolicy.isPrivateChat("5511999999999@broadcast"))
        assertFalse(WhatsAppChatPolicy.isPrivateChat("123456789@newsletter"))
    }
}
