package com.zapbot.android.queue

import com.zapbot.android.data.FileCleanupManager
import com.zapbot.android.database.DownloadJobDao
import com.zapbot.android.domain.DownloadRequest
import com.zapbot.android.domain.DownloadStatus
import com.zapbot.android.domain.DownloadType
import com.zapbot.android.domain.BotMessages
import com.zapbot.android.downloader.MediaDownloader
import com.zapbot.android.logging.BotLogger
import com.zapbot.android.settings.SettingsRepository
import com.zapbot.android.whatsapp.WhatsAppClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.math.max

class DownloadQueueManager(
    private val scope: CoroutineScope,
    private val jobDao: DownloadJobDao,
    private val settingsRepository: SettingsRepository,
    private val downloader: MediaDownloader,
    private val whatsappClient: WhatsAppClient,
    private val cleanup: FileCleanupManager,
    private val logger: BotLogger
) {
    private val queue = Channel<DownloadRequest>(Channel.UNLIMITED)
    private var worker: Job? = null

    fun start() {
        if (worker != null) return
        worker = scope.launch(Dispatchers.IO) {
            val settings = settingsRepository.get()
            jobDao.unfinished().forEach {
                jobDao.finish(it.id, DownloadStatus.FAILED, "Job interrupted by app restart")
            }
            val semaphore = Semaphore(settings.maxConcurrentDownloads.coerceIn(1, max(4, Runtime.getRuntime().availableProcessors())))
            for (request in queue) {
                launch { semaphore.withPermit { process(request) } }
            }
        }
    }

    suspend fun enqueue(request: DownloadRequest) {
        queue.send(request)
    }

    suspend fun cancel(jobId: Long) {
        downloader.cancel(jobId)
        jobDao.finish(jobId, DownloadStatus.CANCELLED, null)
    }

    private suspend fun process(request: DownloadRequest) {
        val outputDir = cleanup.jobDir(request.jobId)
        val settings = settingsRepository.get()
        val text = BotMessages(settings.botLanguage)
        try {
            update(request.jobId, DownloadStatus.DOWNLOADING, 0)
            val flow = when {
                request.isPlaylist -> downloader.downloadPlaylistAudioZip(request.jobId, request.sourceUrl, outputDir, settings.audioBitrate)
                request.type == DownloadType.VIDEO -> downloader.downloadVideo(request.jobId, request.video, outputDir, settings.videoQualityLimit)
                else -> downloader.downloadAudio(request.jobId, request.video, outputDir, settings.audioBitrate)
            }
            var lastProgress = -10
            flow.collectLatest { progress ->
                if (progress.percent == 100 || progress.percent - lastProgress >= 5) {
                    lastProgress = progress.percent
                    update(request.jobId, DownloadStatus.DOWNLOADING, progress.percent)
                }
            }
            update(request.jobId, DownloadStatus.SENDING, 100)
            val result = downloader.resultFile(outputDir)
            validateWhatsAppMedia(request, result.file, text)
            whatsappClient.sendMedia(request.chatId, result.file, text.completedCaption(request.video, request.type, request.isPlaylist), request.messageId)
            jobDao.finish(request.jobId, DownloadStatus.COMPLETED, null)
            alert("✅ *Envio concluído*\n\n_Título:_ ${request.video.title}\n_Tipo:_ ${request.type}\n_Tamanho:_ ${formatSize(result.file.length())}")
            if (settings.deleteFilesAfterSending) cleanup.cleanJob(request.jobId)
            logger.info("Queue", "Job ${request.jobId} completed")
        } catch (t: Throwable) {
            cleanup.cleanJob(request.jobId)
            jobDao.finish(request.jobId, DownloadStatus.FAILED, safeError(t, text))
            alert("🚨 *Erro no download/envio*\n\n_Título:_ ${request.video.title}\n_Motivo:_ ${safeError(t, text)}")
            whatsappClient.sendText(
                request.chatId,
                text.sendFailed(safeError(t, text)),
                request.messageId
            )
            logger.error("Queue", "Job ${request.jobId} failed", t)
        }
    }

    private suspend fun update(jobId: Long, status: DownloadStatus, progress: Int) = withContext(Dispatchers.IO) {
        val current = jobDao.get(jobId) ?: return@withContext
        jobDao.update(current.copy(status = status, progress = progress, updatedAt = System.currentTimeMillis()))
    }

    private fun safeError(t: Throwable, text: BotMessages): String =
        (t.message ?: t.javaClass.simpleName)
            .lineSequence()
            .filterNot { it.contains("older than 90 days", ignoreCase = true) }
            .filterNot { it.contains("It is strongly recommended", ignoreCase = true) }
            .filterNot { it.contains("yt-dlp --update", ignoreCase = true) }
            .filterNot { it.contains("yt-dlp -U", ignoreCase = true) }
            .filterNot { it.startsWith("WARNING:", ignoreCase = true) }
            .joinToString(" ")
            .ifBlank { text.downloaderEmptyFileFallback() }
            .replace(Regex("[\\r\\n]+"), " ")
            .take(180)

    private fun validateWhatsAppMedia(request: DownloadRequest, file: java.io.File, text: BotMessages) {
        if (request.type == DownloadType.VIDEO && file.length() > MAX_WHATSAPP_VIDEO_BYTES) {
            error(text.videoTooLarge(formatSize(file.length()), formatSize(MAX_WHATSAPP_VIDEO_BYTES)))
        }
        if (request.type == DownloadType.VIDEO && file.extension.lowercase() !in VIDEO_EXTENSIONS) {
            error(text.incompatibleVideoFile(file.extension))
        }
        if (request.type == DownloadType.AUDIO && file.extension.lowercase() !in AUDIO_EXTENSIONS) {
            error(text.incompatibleAudioFile(file.extension))
        }
        if (request.isPlaylist && file.extension.lowercase() != "zip") {
            error(text.incompatiblePlaylistFile(file.extension))
        }
    }

    private fun alert(text: String) {
        scope.launch(Dispatchers.IO) {
            runCatching { whatsappClient.sendTextToGroupName(ALERT_GROUP_NAME, text) }
        }
    }

    private fun formatSize(bytes: Long): String {
        val mb = bytes / 1024.0 / 1024.0
        return if (mb >= 1024.0) {
            "%.2f GB".format(mb / 1024.0)
        } else {
            "%.1f MB".format(mb)
        }
    }

    private companion object {
        const val ALERT_GROUP_NAME = "Alerta Zappy"
        const val MAX_WHATSAPP_VIDEO_BYTES = 1536L * 1024L * 1024L
        val VIDEO_EXTENSIONS = setOf("mp4", "webm", "mov", "mkv")
        val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "mp4a", "opus", "ogg", "zip")
    }
}
