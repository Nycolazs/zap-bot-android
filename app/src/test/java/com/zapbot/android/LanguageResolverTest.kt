package com.zapbot.android

import com.zapbot.android.domain.LanguageResolver
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageResolverTest {
    @Test fun resolvesSystemLanguageWhenSupported() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale("pt", "BR"))
            assertEquals("pt", LanguageResolver.resolve("system"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun fallsBackToEnglishWhenSystemLanguageIsUnsupported() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale("fr", "FR"))
            assertEquals("en", LanguageResolver.resolve("system"))
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test fun fallsBackToEnglishForInvalidLanguage() {
        assertEquals("en", LanguageResolver.resolve("invalid"))
    }
}
