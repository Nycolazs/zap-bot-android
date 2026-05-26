package com.zapbot.android.database

import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        BotSettingsEntity::class,
        ChatSearchSessionEntity::class,
        DownloadJobEntity::class,
        BotLogEntity::class
    ],
    version = 4
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): BotSettingsDao
    abstract fun searchSessionDao(): ChatSearchSessionDao
    abstract fun downloadJobDao(): DownloadJobDao
    abstract fun logDao(): BotLogDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bot_settings ADD COLUMN themeMode TEXT NOT NULL DEFAULT 'system'")
                db.execSQL("UPDATE bot_settings SET deleteFilesAfterSending = 1")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("UPDATE bot_settings SET maxConcurrentDownloads = 5 WHERE maxConcurrentDownloads < 5")
                db.execSQL("UPDATE bot_settings SET videoQualityLimit = '360p' WHERE videoQualityLimit != '360p'")
                db.execSQL("UPDATE bot_settings SET deleteFilesAfterSending = 1")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE bot_settings ADD COLUMN appLanguage TEXT NOT NULL DEFAULT 'en'")
                db.execSQL("ALTER TABLE bot_settings ADD COLUMN botLanguage TEXT NOT NULL DEFAULT 'pt'")
            }
        }
    }
}
