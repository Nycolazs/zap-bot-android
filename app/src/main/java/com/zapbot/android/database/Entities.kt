package com.zapbot.android.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.zapbot.android.domain.DownloadStatus
import com.zapbot.android.domain.DownloadType
import com.zapbot.android.domain.LogLevel

@Entity(tableName = "bot_settings")
data class BotSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val autoStartOnBoot: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val detailedNotificationsEnabled: Boolean = true,
    val maxConcurrentDownloads: Int = 5,
    val videoQualityLimit: String = "360p",
    val audioBitrate: String = "128k",
    val deleteFilesAfterSending: Boolean = true,
    val themeMode: String = "system",
    val appLanguage: String = "en",
    val botLanguage: String = "pt",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_search_sessions")
data class ChatSearchSessionEntity(
    @PrimaryKey val chatId: String,
    val query: String,
    val resultsJson: String,
    val createdAt: Long,
    val expiresAt: Long
)

@Entity(tableName = "download_jobs")
data class DownloadJobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chatId: String,
    val messageId: String,
    val youtubeVideoId: String,
    val title: String,
    val type: DownloadType,
    val status: DownloadStatus,
    val progress: Int = 0,
    val errorMessage: String? = null,
    val outputFilePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(tableName = "bot_logs")
data class BotLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val throwable: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
