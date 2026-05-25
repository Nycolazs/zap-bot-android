package com.zapbot.android.downloader

import android.content.Context
import com.zapbot.android.domain.DownloadResult
import com.zapbot.android.domain.YouTubeVideoResult
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLResponse
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

class YtDlpMediaDownloader(private val appContext: Context) : MediaDownloader {
    private val initMutex = Mutex()
    private var initialized = false

    override fun downloadVideo(jobId: Long, video: YouTubeVideoResult, outputDir: File, qualityLimit: String): Flow<DownloadProgress> = flow {
        ensureInitialized()
        outputDir.mkdirs()
        clearOutput(outputDir)
        emit(DownloadProgress(0, "Preparando download"))
        val height = (qualityLimit.filter(Char::isDigit).toIntOrNull() ?: 360).coerceAtMost(SAFE_VIDEO_HEIGHT)
        val request = baseRequest(video, outputDir).apply {
            addOption(
                "-f",
                "best[height<=$height][ext=mp4][filesize<=$SAFE_VIDEO_UPLOAD_BYTES]/best[height<=$height][ext=mp4][filesize_approx<=$SAFE_VIDEO_UPLOAD_BYTES]/worst[ext=mp4][filesize<=$SAFE_VIDEO_UPLOAD_BYTES]/worst[ext=mp4][filesize_approx<=$SAFE_VIDEO_UPLOAD_BYTES]/worst[ext=mp4]/worst"
            )
            addOption("--merge-output-format", "mp4")
            addOption("-S", "res:$height,ext:mp4:m4a,+size")
        }
        emit(DownloadProgress(5, "Baixando vídeo"))
        executeAndValidate(request, processId(jobId), outputDir)
        emit(DownloadProgress(100, "Vídeo baixado"))
    }

    override fun downloadAudio(jobId: Long, video: YouTubeVideoResult, outputDir: File, bitrate: String): Flow<DownloadProgress> = flow {
        ensureInitialized()
        outputDir.mkdirs()
        clearOutput(outputDir)
        emit(DownloadProgress(0, "Preparando download"))
        val request = baseRequest(video, outputDir).apply {
            addOption("-f", "bestaudio/best")
            addOption("-x")
            addOption("--audio-format", "mp3")
            addOption("--audio-quality", bitrate.uppercase())
            addOption("--add-metadata")
            addOption("--embed-thumbnail")
            addOption("--convert-thumbnails", "jpg")
        }
        emit(DownloadProgress(5, "Baixando áudio"))
        executeAndValidate(request, processId(jobId), outputDir)
        emit(DownloadProgress(100, "Áudio baixado"))
    }

    override suspend fun resultFile(outputDir: File): DownloadResult = withContext(Dispatchers.IO) {
        val file = outputDir.listFiles()
            ?.filter { it.isFile && it.length() > 0L && !it.name.endsWith(".part") && it.extension.lowercase() in MEDIA_EXTENSIONS }
            ?.maxByOrNull { it.lastModified() }
            ?: error("Arquivo baixado não encontrado")
        DownloadResult(file, mimeType(file))
    }

    override suspend fun cancel(jobId: Long) {
        YoutubeDL.getInstance().destroyProcessById(processId(jobId))
    }

    private fun baseRequest(video: YouTubeVideoResult, outputDir: File): YoutubeDLRequest =
        YoutubeDLRequest(video.url).apply {
            addOption("--no-playlist")
            addOption("--newline")
            addOption("--no-mtime")
            addOption("--no-warnings")
            addOption("--force-ipv4")
            addOption("--extractor-args", "youtube:player_client=android,ios,web")
            addOption("--user-agent", YOUTUBE_USER_AGENT)
            addOption("--referer", "https://www.youtube.com/")
            addOption("--restrict-filenames")
            addOption("-o", File(outputDir, "%(title).80B.%(ext)s").absolutePath)
        }

    private suspend fun executeAndValidate(request: YoutubeDLRequest, processId: String, outputDir: File) {
        try {
            validateResponse(execute(request, processId), outputDir)
        } catch (t: YoutubeDLException) {
            if (hasDownloadedFile(outputDir) || t.isOnlyOldVersionWarning()) return
            throw t
        }
    }

    private suspend fun execute(request: YoutubeDLRequest, processId: String): YoutubeDLResponse = withContext(Dispatchers.IO) {
        YoutubeDL.getInstance().execute(request, processId)
    }

    private fun validateResponse(response: YoutubeDLResponse, outputDir: File) {
        if (response.exitCode == 0 || hasDownloadedFile(outputDir)) return
        val error = response.err.ifBlank { response.out }.withoutYtDlpWarnings()
            .ifBlank { "yt-dlp não gerou nenhum arquivo de saída" }
        error(error.take(220))
    }

    private suspend fun ensureInitialized() {
        if (initialized) return
        initMutex.withLock {
            if (!initialized) {
                YoutubeDL.getInstance().init(appContext)
                FFmpeg.getInstance().init(appContext)
                initialized = true
            }
        }
    }

    private fun clearOutput(outputDir: File) {
        outputDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun hasDownloadedFile(outputDir: File): Boolean =
        outputDir.listFiles()?.any { it.isFile && it.length() > 0L && !it.name.endsWith(".part") } == true

    private fun Throwable.isOnlyOldVersionWarning(): Boolean {
        val message = (message ?: "").withoutYtDlpWarnings()
        return message.isBlank()
    }

    private fun String.withoutYtDlpWarnings(): String =
        lineSequence()
            .filterNot { it.contains("older than 90 days", ignoreCase = true) }
            .filterNot { it.contains("It is strongly recommended", ignoreCase = true) }
            .filterNot { it.contains("yt-dlp --update", ignoreCase = true) }
            .filterNot { it.contains("yt-dlp -U", ignoreCase = true) }
            .filterNot { it.startsWith("WARNING:", ignoreCase = true) }
            .joinToString(" ")
            .trim()

    private fun processId(jobId: Long): String = "job-$jobId"

    private fun mimeType(file: File): String = when (file.extension.lowercase()) {
        "mp4" -> "video/mp4"
        "m4a", "mp4a" -> "audio/mp4"
        "mp3" -> "audio/mpeg"
        "opus", "ogg" -> "audio/ogg"
        "webm" -> "video/webm"
        else -> "application/octet-stream"
    }

    private companion object {
        const val SAFE_VIDEO_HEIGHT = 360
        const val SAFE_VIDEO_UPLOAD_BYTES = 55L * 1024L * 1024L
        val MEDIA_EXTENSIONS = setOf("mp4", "m4a", "mp4a", "mp3", "opus", "ogg", "webm")
        const val YOUTUBE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Mobile Safari/537.36"
    }
}
