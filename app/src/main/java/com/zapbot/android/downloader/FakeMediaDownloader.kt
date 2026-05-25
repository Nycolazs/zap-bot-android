package com.zapbot.android.downloader

import com.zapbot.android.domain.DownloadResult
import com.zapbot.android.domain.YouTubeVideoResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

class FakeMediaDownloader : MediaDownloader {
    override fun downloadVideo(jobId: Long, video: YouTubeVideoResult, outputDir: File, qualityLimit: String): Flow<DownloadProgress> = fake(outputDir, video, "mp4")
    override fun downloadAudio(jobId: Long, video: YouTubeVideoResult, outputDir: File, bitrate: String): Flow<DownloadProgress> = fake(outputDir, video, "m4a")

    private fun fake(outputDir: File, video: YouTubeVideoResult, extension: String) = flow {
        outputDir.mkdirs()
        for (progress in listOf(5, 25, 50, 75, 100)) {
            delay(80)
            emit(DownloadProgress(progress, "Preparando $progress%"))
        }
        File(outputDir, "${safeFilename(video.title)}.$extension").writeText("fake media for ${video.url}")
    }

    override suspend fun resultFile(outputDir: File): DownloadResult {
        val file = outputDir.listFiles()?.firstOrNull { it.isFile } ?: error("Output file not found")
        val mime = if (file.extension == "mp4") "video/mp4" else "audio/mp4"
        return DownloadResult(file, mime)
    }

    override suspend fun cancel(jobId: Long) = Unit
}
