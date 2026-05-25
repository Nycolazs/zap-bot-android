package com.zapbot.android.youtube

import com.squareup.moshi.Json
import com.zapbot.android.domain.YouTubeVideoResult
import retrofit2.http.GET
import retrofit2.http.Query

class YouTubeApiClient(
    private val api: YouTubeApi,
    private val apiKey: String
) : YouTubeSearchClient {
    override suspend fun searchVideos(query: String, maxResults: Int): List<YouTubeVideoResult> {
        require(apiKey.isNotBlank()) { "YouTube API key is not configured" }
        val search = api.search(
            key = apiKey,
            q = query,
            maxResults = 20
        )
        val ids = search.items.mapNotNull { it.id.videoId }.distinct()
        if (ids.isEmpty()) return emptyList()
        val videos = api.videos(key = apiKey, id = ids.joinToString(","))
        return videos.items.asSequence()
            .mapNotNull { item ->
                val duration = parseIsoDurationSeconds(item.contentDetails?.duration ?: return@mapNotNull null)
                val snippet = item.snippet ?: return@mapNotNull null
                if (!YouTubeVideoFilter.isAllowed(duration, item.status?.privacyStatus, item.liveStreamingDetails != null, snippet.liveBroadcastContent)) {
                    return@mapNotNull null
                }
                YouTubeVideoResult(
                    title = snippet.title.orEmpty(),
                    channel = snippet.channelTitle.orEmpty(),
                    videoId = item.id,
                    url = "https://youtube.com/watch?v=${item.id}",
                    durationSeconds = duration,
                    thumbnailUrl = snippet.thumbnails?.medium?.url ?: snippet.thumbnails?.default?.url
                )
            }
            .take(maxResults)
            .toList()
    }

    private fun parseIsoDurationSeconds(value: String): Long {
        return runCatching { java.time.Duration.parse(value).seconds }.getOrDefault(0)
    }
}

interface YouTubeApi {
    @GET("youtube/v3/search")
    suspend fun search(
        @Query("key") key: String,
        @Query("part") part: String = "snippet",
        @Query("q") q: String,
        @Query("type") type: String = "video",
        @Query("safeSearch") safeSearch: String = "moderate",
        @Query("videoEmbeddable") videoEmbeddable: String = "true",
        @Query("videoSyndicated") videoSyndicated: String = "true",
        @Query("maxResults") maxResults: Int
    ): SearchResponse

    @GET("youtube/v3/videos")
    suspend fun videos(
        @Query("key") key: String,
        @Query("part") part: String = "contentDetails,status,snippet,liveStreamingDetails",
        @Query("id") id: String
    ): VideosResponse
}

data class SearchResponse(val items: List<SearchItem> = emptyList())
data class SearchItem(val id: SearchId = SearchId())
data class SearchId(val videoId: String? = null)
data class VideosResponse(val items: List<VideoItem> = emptyList())
data class VideoItem(
    val id: String,
    val snippet: VideoSnippet? = null,
    val contentDetails: ContentDetails? = null,
    val status: VideoStatus? = null,
    val liveStreamingDetails: Map<String, Any>? = null
)
data class VideoSnippet(
    val title: String? = null,
    val channelTitle: String? = null,
    val liveBroadcastContent: String? = null,
    val thumbnails: Thumbnails? = null
)
data class Thumbnails(@Json(name = "default") val default: Thumbnail? = null, val medium: Thumbnail? = null)
data class Thumbnail(val url: String? = null)
data class ContentDetails(val duration: String? = null)
data class VideoStatus(val privacyStatus: String? = null)
