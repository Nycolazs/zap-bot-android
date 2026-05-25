package com.zapbot.android.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        BotSettingsEntity::class,
        ChatSearchSessionEntity::class,
        DownloadJobEntity::class,
        BotLogEntity::class
    ],
    version = 1
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): BotSettingsDao
    abstract fun searchSessionDao(): ChatSearchSessionDao
    abstract fun downloadJobDao(): DownloadJobDao
    abstract fun logDao(): BotLogDao
}
