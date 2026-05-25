package com.zapbot.android

import com.zapbot.android.domain.YouTubeVideoResult
import org.junit.Assert.assertEquals
import org.junit.Test

class YouTubeVideoResultTest {
    @Test fun formatsDurationsUnderOneHourAsMinutes() {
        assertEquals("13:50", result(830).durationText)
    }

    @Test fun formatsDurationsOverOneHourWithHours() {
        assertEquals("1:13:50", result(4_430).durationText)
    }

    private fun result(duration: Long) = YouTubeVideoResult(
        title = "Video",
        channel = "Channel",
        videoId = "id",
        url = "https://youtube.com/watch?v=id",
        durationSeconds = duration,
        thumbnailUrl = null
    )
}
