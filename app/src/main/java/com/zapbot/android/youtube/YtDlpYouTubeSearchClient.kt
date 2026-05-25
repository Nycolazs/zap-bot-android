package com.zapbot.android.youtube

import android.content.Context
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.zapbot.android.domain.YouTubeVideoResult
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class YtDlpYouTubeSearchClient(
    private val appContext: Context,
    moshi: Moshi
) : YouTubeSearchClient {
    private val initMutex = Mutex()
    private var initialized = false
    private val resultAdapter = moshi.adapter<YtDlpSearchResult>(
        YtDlpSearchResult::class.java
    )

    override suspend fun searchVideos(query: String, maxResults: Int): List<YouTubeVideoResult> = withContext(Dispatchers.IO) {
        ensureInitialized()
        val limit = maxResults.coerceIn(1, 10)
        val request = YoutubeDLRequest("ytsearch$limit:$query").apply {
            addOption("--dump-json")
            addOption("--skip-download")
            addOption("--ignore-errors")
        }
        val output = YoutubeDL.getInstance().execute(request).out
        output.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("{") }
            .mapNotNull { line -> runCatching { resultAdapter.fromJson(line) }.getOrNull() }
            .mapNotNull { item -> item.toVideoResult() }
            .filter { it.durationSeconds > 0 }
            .filter { it.durationSeconds <= MAX_SEARCH_DURATION_SECONDS }
            .take(limit)
            .toList()
    }

    private suspend fun ensureInitialized() {
        if (initialized) return
        initMutex.withLock {
            if (!initialized) {
                YoutubeDL.getInstance().init(appContext)
                initialized = true
            }
        }
    }
}

private const val MAX_SEARCH_DURATION_SECONDS = 70L * 60L

private data class YtDlpSearchResult(
    val id: String? = null,
    val title: String? = null,
    val duration: Double? = null,
    val uploader: String? = null,
    val channel: String? = null,
    @Json(name = "webpage_url") val webpageUrl: String? = null,
    @Json(name = "thumbnail") val thumbnail: String? = null,
    @Json(name = "upload_date") val uploadDate: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null
) {
    fun toVideoResult(): YouTubeVideoResult? {
        val videoId = id?.takeIf { it.isNotBlank() } ?: return null
        val safeTitle = title?.takeIf { it.isNotBlank() } ?: return null
        val url = webpageUrl?.takeIf { it.isNotBlank() } ?: "https://www.youtube.com/watch?v=$videoId"
        return YouTubeVideoResult(
            title = safeTitle,
            channel = channel?.takeIf { it.isNotBlank() } ?: uploader.orEmpty(),
            videoId = videoId,
            url = url,
            durationSeconds = duration?.toLong() ?: 0L,
            thumbnailUrl = thumbnail,
            publishedText = formattedDate(releaseDate ?: uploadDate)
        )
    }

    private fun formattedDate(raw: String?): String? {
        if (raw.isNullOrBlank() || raw.length != 8) return null
        return "${raw.substring(6, 8)}/${raw.substring(4, 6)}/${raw.substring(0, 4)}"
    }
}
