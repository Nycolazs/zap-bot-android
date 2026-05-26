package com.zapbot.android.updates

import com.zapbot.android.domain.LanguageResolver

class UpdateChecker {
    suspend fun check(language: String): UpdateCheckResult =
        UpdateCheckResult.NotConfigured(
            when (LanguageResolver.resolve(language)) {
                "pt" -> "A verificação de atualizações ainda não está configurada."
                "es" -> "La búsqueda de actualizaciones aún no está configurada."
                "ru" -> "Проверка обновлений пока не настроена."
                else -> "Update checking is not configured yet."
            }
        )
}

sealed interface UpdateCheckResult {
    data class NotConfigured(val message: String) : UpdateCheckResult
}
