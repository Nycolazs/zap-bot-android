package com.zapbot.android.logging

import com.zapbot.android.database.BotLogDao
import com.zapbot.android.database.BotLogEntity
import com.zapbot.android.domain.LogLevel

class BotLogger(private val dao: BotLogDao) {
    suspend fun debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
    suspend fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)
    suspend fun warn(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.WARN, tag, message, throwable)
    suspend fun error(tag: String, message: String, throwable: Throwable? = null) = log(LogLevel.ERROR, tag, message, throwable)

    private suspend fun log(level: LogLevel, tag: String, message: String, throwable: Throwable? = null) {
        dao.insert(
            BotLogEntity(
                level = level,
                tag = tag.take(48),
                message = message.take(1_000),
                throwable = throwable?.javaClass?.simpleName?.let { "$it: ${throwable.message}".take(1_000) }
            )
        )
    }
}
