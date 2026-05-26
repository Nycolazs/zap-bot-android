package com.zapbot.android

import android.app.Application
import androidx.room.Room
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.zapbot.android.data.FileCleanupManager
import com.zapbot.android.data.SearchSessionRepository
import com.zapbot.android.database.AppDatabase
import com.zapbot.android.downloader.YtDlpMediaDownloader
import com.zapbot.android.logging.BotLogger
import com.zapbot.android.network.NetworkMonitor
import com.zapbot.android.notifications.BotNotificationManager
import com.zapbot.android.settings.SettingsRepository
import com.zapbot.android.updates.UpdateChecker
import com.zapbot.android.whatsapp.WhatsAppClient
import com.zapbot.android.whatsapp.WhatsmeowWhatsAppClient
import com.zapbot.android.youtube.YouTubeWebSearchClient
import com.zapbot.android.youtube.YouTubeSearchClient

class ZapBotApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.cleanup.cleanOldTempFiles()
    }
}

class AppContainer(app: Application) {
    val database: AppDatabase = Room.databaseBuilder(app, AppDatabase::class.java, "zapbot.db")
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
        .build()
    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    val settings = SettingsRepository(database.settingsDao())
    val logger = BotLogger(database.logDao())
    val sessions = SearchSessionRepository(database.searchSessionDao(), moshi)
    val cleanup = FileCleanupManager(app.cacheDir)
    val notifications = BotNotificationManager(app)
    val networkMonitor = NetworkMonitor(app)
    val updateChecker = UpdateChecker()

    val whatsappClient: WhatsAppClient = WhatsmeowWhatsAppClient(app)
    val youtubeClient: YouTubeSearchClient = YouTubeWebSearchClient(moshi)
    val downloader = YtDlpMediaDownloader(app)
}
