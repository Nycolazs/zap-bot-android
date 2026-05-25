package com.zapbot.android.youtube

import com.zapbot.android.domain.YouTubeVideoResult

class FakeYouTubeSearchClient : YouTubeSearchClient {
    override suspend fun searchVideos(query: String, maxResults: Int): List<YouTubeVideoResult> =
        (1..maxResults).map {
            YouTubeVideoResult(
                title = "$query resultado $it",
                channel = "Channel $it",
                videoId = "fake$it",
                url = "https://youtube.com/watch?v=fake$it",
                durationSeconds = 120L + it,
                thumbnailUrl = null,
                publishedText = "01/01/2026"
            )
        }
}
