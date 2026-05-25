package com.zapbot.android.settings

import com.zapbot.android.database.BotSettingsDao
import com.zapbot.android.database.BotSettingsEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dao: BotSettingsDao) {
    val settings: Flow<BotSettingsEntity> = dao.observe().map { it ?: BotSettingsEntity() }

    suspend fun get(): BotSettingsEntity = dao.get() ?: BotSettingsEntity().also { dao.upsert(it) }

    suspend fun update(transform: (BotSettingsEntity) -> BotSettingsEntity) {
        val current = get()
        dao.upsert(transform(current).copy(updatedAt = System.currentTimeMillis()))
    }
}
