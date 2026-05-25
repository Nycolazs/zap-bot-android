package com.zapbot.android.downloader

import com.zapbot.android.domain.DownloadResult
import com.zapbot.android.domain.YouTubeVideoResult
import kotlinx.coroutines.flow.Flow
import java.io.File

data class DownloadProgress(val percent: Int, val message: String)

interface MediaDownloader {
    fun downloadVideo(jobId: Long, video: YouTubeVideoResult, outputDir: File, qualityLimit: String): Flow<DownloadProgress>
    fun downloadAudio(jobId: Long, video: YouTubeVideoResult, outputDir: File, bitrate: String): Flow<DownloadProgress>
    suspend fun resultFile(outputDir: File): DownloadResult
    suspend fun cancel(jobId: Long)
}
