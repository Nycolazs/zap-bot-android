package com.zapbot.android.domain

import java.util.Locale

object LanguageResolver {
    const val SYSTEM = "system"
    val supported = setOf("en", "pt", "es", "ru", SYSTEM)

    fun resolve(language: String): String {
        if (language != SYSTEM) return language.takeIf { it in supported && it != SYSTEM } ?: "en"
        return Locale.getDefault().language
            .lowercase(Locale.US)
            .takeIf { it in supported && it != SYSTEM }
            ?: "en"
    }
}
