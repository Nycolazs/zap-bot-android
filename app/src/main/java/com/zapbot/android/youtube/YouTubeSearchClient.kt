package com.zapbot.android.youtube

import com.zapbot.android.domain.YouTubeVideoResult

interface YouTubeSearchClient {
    suspend fun searchVideos(query: String, maxResults: Int = 8): List<YouTubeVideoResult>
}
