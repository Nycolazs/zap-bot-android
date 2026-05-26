package com.zapbot.android.settings

import com.zapbot.android.database.BotSettingsDao
import com.zapbot.android.database.BotSettingsEntity
import com.zapbot.android.domain.LanguageResolver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dao: BotSettingsDao) {
    val settings: Flow<BotSettingsEntity> = dao.observe().map {
        (it ?: BotSettingsEntity()).normalized()
    }

    suspend fun get(): BotSettingsEntity =
        (dao.get() ?: BotSettingsEntity().also { dao.upsert(it) }).normalized()

    suspend fun update(transform: (BotSettingsEntity) -> BotSettingsEntity) {
        val current = get()
        dao.upsert(transform(current).normalized().copy(updatedAt = System.currentTimeMillis()))
    }

    private fun BotSettingsEntity.normalized(): BotSettingsEntity =
        copy(
            deleteFilesAfterSending = true,
            maxConcurrentDownloads = maxConcurrentDownloads.coerceIn(1, 8),
            appLanguage = appLanguage.takeIf { it in LanguageResolver.supported } ?: LanguageResolver.SYSTEM,
            botLanguage = botLanguage.takeIf { it in LanguageResolver.supported } ?: LanguageResolver.SYSTEM,
            networkPreference = networkPreference.takeIf { it in NETWORK_PREFERENCES } ?: "WIFI_ONLY"
        )

    private companion object {
        val NETWORK_PREFERENCES = setOf("WIFI_ONLY", "ANY_NETWORK")
    }
}
