package com.zapbot.android.domain

import java.io.File

data class YouTubeVideoResult(
    val title: String,
    val channel: String,
    val videoId: String,
    val url: String,
    val durationSeconds: Long,
    val thumbnailUrl: String?,
    val publishedText: String? = null
) {
    val durationText: String
        get() {
            val hours = durationSeconds / 3_600
            val minutes = (durationSeconds % 3_600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else {
                "%02d:%02d".format(minutes, seconds)
            }
        }
}

enum class DownloadType { VIDEO, AUDIO }
enum class DownloadStatus { QUEUED, DOWNLOADING, PROCESSING, SENDING, COMPLETED, FAILED, CANCELLED }
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

sealed interface BotCommand {
    data object Help : BotCommand
    data class Search(val query: String) : BotCommand
    data class DownloadVideo(val index: Int) : BotCommand
    data class DownloadAudio(val index: Int) : BotCommand
    data class DownloadVideoLink(val url: String) : BotCommand
    data class DownloadAudioLink(val url: String) : BotCommand
    data object Status : BotCommand
    data object Cancel : BotCommand
    data class Invalid(val reason: String) : BotCommand
    data object Unknown : BotCommand
}

data class IncomingWhatsAppMessage(
    val id: String,
    val chatId: String,
    val senderName: String?,
    val text: String,
    val quotedMessageId: String?,
    val quotedText: String?,
    val timestamp: Long
)

sealed interface WhatsAppConnectionState {
    data object Disconnected : WhatsAppConnectionState
    data class WaitingForQr(val qrData: String) : WhatsAppConnectionState
    data object Connecting : WhatsAppConnectionState
    data class Connected(val phoneNumber: String?) : WhatsAppConnectionState
    data object Running : WhatsAppConnectionState
    data class Error(val message: String, val throwable: Throwable? = null) : WhatsAppConnectionState
}

data class DownloadRequest(
    val jobId: Long,
    val chatId: String,
    val messageId: String,
    val video: YouTubeVideoResult,
    val type: DownloadType,
    val sourceUrl: String = video.url,
    val isPlaylist: Boolean = false
)

data class DownloadResult(
    val file: File,
    val mimeType: String
)
