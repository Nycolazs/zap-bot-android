package com.zapbot.android.downloader

import android.content.Context
import com.zapbot.android.domain.DownloadResult
import com.zapbot.android.domain.MediaUrl
import com.zapbot.android.domain.MediaUrlParser
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
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class YtDlpMediaDownloader(private val appContext: Context) : MediaDownloader {
    private val initMutex = Mutex()
    private var initialized = false

    override fun downloadVideo(jobId: Long, video: YouTubeVideoResult, outputDir: File, qualityLimit: String): Flow<DownloadProgress> = flow {
        ensureInitialized()
        outputDir.mkdirs()
        clearOutput(outputDir)
        emit(DownloadProgress(0, "Preparando download"))
        val height = (qualityLimit.filter(Char::isDigit).toIntOrNull() ?: 360).coerceAtMost(SAFE_VIDEO_HEIGHT)
        val mediaUrl = MediaUrlParser.parse(video.url)
        val request = baseRequest(video.url, outputDir).apply {
            if (mediaUrl?.source == MediaUrl.Source.INSTAGRAM || mediaUrl?.source == MediaUrl.Source.TIKTOK) {
                addOption(
                    "-f",
                    "best[ext=mp4][vcodec!=none][acodec!=none][filesize<=$SAFE_VIDEO_UPLOAD_BYTES]/best[ext=mp4][vcodec!=none][acodec!=none][filesize_approx<=$SAFE_VIDEO_UPLOAD_BYTES]/bestvideo*[ext=mp4][filesize<=$SAFE_VIDEO_UPLOAD_BYTES]+bestaudio[ext=m4a]/best[filesize<=$SAFE_VIDEO_UPLOAD_BYTES]/best[filesize_approx<=$SAFE_VIDEO_UPLOAD_BYTES]"
                )
                addOption("--merge-output-format", "mp4")
                addOption("--recode-video", "mp4")
                addOption("-S", "hasaud,ext:mp4:m4a,+size,+br")
            } else {
                addOption(
                    "-f",
                    "best[height<=$height][ext=mp4][vcodec!=none][acodec!=none][filesize<=$SAFE_VIDEO_UPLOAD_BYTES]/best[height<=$height][ext=mp4][vcodec!=none][acodec!=none][filesize_approx<=$SAFE_VIDEO_UPLOAD_BYTES]/worst[ext=mp4][vcodec!=none][acodec!=none]/bestvideo[height<=$height][ext=mp4][filesize<=$SAFE_VIDEO_UPLOAD_BYTES]+bestaudio[ext=m4a]/bestvideo[height<=$height][ext=mp4]+bestaudio[ext=m4a]"
                )
                addOption("--merge-output-format", "mp4")
                addOption("-S", "res:$height,hasaud,ext:mp4:m4a,+size")
            }
            addOption("--max-filesize", SAFE_VIDEO_UPLOAD_BYTES.toString())
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
        val request = baseRequest(video.url, outputDir).apply {
            addOption("-f", "bestaudio[acodec!=none][vcodec=none]/bestaudio[acodec!=none]/best[acodec!=none]")
            addOption("--extract-audio")
            addOption("--audio-format", "mp3")
            addOption("--audio-quality", bitrate)
            addOption("-S", "+size")
        }
        emit(DownloadProgress(5, "Baixando áudio"))
        executeAndValidate(request, processId(jobId), outputDir)
        emit(DownloadProgress(100, "Áudio baixado"))
    }

    override fun downloadPlaylistAudioZip(jobId: Long, playlistUrl: String, outputDir: File, bitrate: String): Flow<DownloadProgress> = flow {
        ensureInitialized()
        outputDir.mkdirs()
        clearOutput(outputDir)
        val audioDir = File(outputDir, "playlist-audio").apply { mkdirs() }
        emit(DownloadProgress(0, "Preparando playlist"))
        val request = baseRequest(playlistUrl, audioDir, noPlaylist = false).apply {
            addOption("--yes-playlist")
            addOption("--ignore-errors")
            addOption("--playlist-end", MAX_PLAYLIST_ITEMS.toString())
            addOption("-f", "bestaudio[acodec!=none][vcodec=none]/bestaudio[acodec!=none]/best[acodec!=none]")
            addOption("--extract-audio")
            addOption("--audio-format", "mp3")
            addOption("--audio-quality", bitrate)
            addOption("-S", "+size")
            addOption("-o", File(audioDir, "%(playlist_index)03d-%(title).80B.%(ext)s").absolutePath)
        }
        emit(DownloadProgress(5, "Baixando áudios da playlist"))
        executeAndValidate(request, processId(jobId), audioDir)
        val audioFiles = audioDir.listFiles()
            ?.filter { it.isFile && it.length() > 0L && it.extension.lowercase() in AUDIO_EXTENSIONS }
            ?.sortedBy { it.name }
            .orEmpty()
        if (audioFiles.isEmpty()) error("Nenhum áudio da playlist pôde ser baixado com sucesso")
        emit(DownloadProgress(90, "Compactando playlist"))
        zipFiles(audioFiles, File(outputDir, "playlist-audios.zip"))
        emit(DownloadProgress(100, "Playlist pronta"))
    }

    override suspend fun resultFile(outputDir: File): DownloadResult = withContext(Dispatchers.IO) {
        val file = outputDir.walkTopDown()
            ?.filter { it.isFile && it.length() > 0L && !it.name.endsWith(".part") && it.extension.lowercase() in MEDIA_EXTENSIONS }
            ?.maxByOrNull { it.lastModified() }
            ?: error("Arquivo baixado não encontrado")
        if (file.extension.isBlank()) {
            error("Arquivo baixado sem extensão de mídia válida")
        }
        val mimeType = mimeType(file)
        if (mimeType == "application/octet-stream") {
            error("Arquivo baixado com tipo de mídia desconhecido: ${file.name}")
        }
        DownloadResult(file, mimeType)
    }

    override suspend fun cancel(jobId: Long) {
        YoutubeDL.getInstance().destroyProcessById(processId(jobId))
    }

    private fun baseRequest(url: String, outputDir: File, noPlaylist: Boolean = true): YoutubeDLRequest =
        YoutubeDLRequest(url).apply {
            val mediaUrl = MediaUrlParser.parse(url)
            if (noPlaylist) addOption("--no-playlist")
            addOption("--newline")
            addOption("--no-mtime")
            addOption("--no-warnings")
            addOption("--concurrent-fragments", "8")
            addOption("--retries", "5")
            addOption("--fragment-retries", "5")
            addOption("--extractor-retries", "3")
            addOption("--retry-sleep", "fragment:exp=1:20")
            addOption("--socket-timeout", "30")
            addOption("--trim-filenames", "120")
            addOption("--geo-bypass")
            when (mediaUrl?.source) {
                MediaUrl.Source.INSTAGRAM -> {
                    addOption("--user-agent", MOBILE_USER_AGENT)
                    addOption("--referer", "https://www.instagram.com/")
                }
                MediaUrl.Source.TIKTOK -> {
                    addOption("--user-agent", MOBILE_USER_AGENT)
                    addOption("--referer", "https://www.tiktok.com/")
                }
                else -> {
                    addOption("--extractor-args", "youtube:player_client=android,ios,web")
                    addOption("--user-agent", YOUTUBE_USER_AGENT)
                    addOption("--referer", "https://www.youtube.com/")
                }
            }
            addOption("--restrict-filenames")
            addOption("-o", File(outputDir, "%(title).80B-%(id)s.%(ext)s").absolutePath)
        }

    private suspend fun zipFiles(files: List<File>, zipFile: File) = withContext(Dispatchers.IO) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            files.forEach { file ->
                FileInputStream(file).use { input ->
                    zip.putNextEntry(ZipEntry(file.name))
                    input.copyTo(zip)
                    zip.closeEntry()
                }
            }
        }
    }

    private suspend fun executeAndValidate(request: YoutubeDLRequest, processId: String, outputDir: File) {
        try {
            validateResponse(execute(request, processId), outputDir)
        } catch (t: YoutubeDLException) {
            if (hasDownloadedFile(outputDir)) return
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
                updateYoutubeDlIfStale()
                initialized = true
            }
        }
    }

    private fun updateYoutubeDlIfStale() {
        val prefs = appContext.getSharedPreferences("zappy_downloader", Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong("last_ytdlp_update_at", 0L) < YTDLP_UPDATE_INTERVAL_MS) return
        val updated = runCatching {
            val instance = YoutubeDL.getInstance()
            val method = instance.javaClass.methods.firstOrNull { it.name == "updateYoutubeDL" }
            when (method?.parameterTypes?.size) {
                1 -> method.invoke(instance, appContext)
                2 -> {
                    val channelType = method.parameterTypes[1]
                    val channel = channelType.enumConstants?.firstOrNull {
                        it.toString().equals("STABLE", ignoreCase = true)
                    } ?: channelType.enumConstants?.firstOrNull()
                    method.invoke(instance, appContext, channel)
                }
                else -> null
            }
            method != null
        }.getOrDefault(false)
        if (updated) {
            prefs.edit().putLong("last_ytdlp_update_at", now).apply()
        }
    }

    private fun clearOutput(outputDir: File) {
        outputDir.listFiles()?.forEach { it.deleteRecursively() }
    }

    private fun hasDownloadedFile(outputDir: File): Boolean =
        outputDir.walkTopDown().any { it.isFile && it.length() > 0L && !it.name.endsWith(".part") && it.extension.lowercase() in MEDIA_EXTENSIONS }

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
        "mov" -> "video/quicktime"
        "mkv" -> "video/x-matroska"
        "zip" -> "application/zip"
        else -> "application/octet-stream"
    }

    private companion object {
        const val SAFE_VIDEO_HEIGHT = 360
        const val SAFE_VIDEO_UPLOAD_BYTES = 15L * 1024L * 1024L
        const val YTDLP_UPDATE_INTERVAL_MS = 24L * 60L * 60L * 1000L
        const val MAX_PLAYLIST_ITEMS = 50
        val AUDIO_EXTENSIONS = setOf("m4a", "mp4a", "mp3", "opus", "ogg")
        val MEDIA_EXTENSIONS = setOf("mp4", "m4a", "mp4a", "mp3", "opus", "ogg", "webm", "mov", "mkv", "zip")
        const val YOUTUBE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Mobile Safari/537.36"
        const val MOBILE_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 12) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125 Mobile Safari/537.36"
    }
}
