package com.zapbot.android

import com.zapbot.android.youtube.YouTubeVideoFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeVideoFilterTest {
    @Test fun rejectsShortsAndLiveVideos() {
        assertFalse(YouTubeVideoFilter.isAllowed(60, "public", false, "none"))
        assertFalse(YouTubeVideoFilter.isAllowed(120, "public", true, "none"))
        assertFalse(YouTubeVideoFilter.isAllowed(120, "public", false, "live"))
    }

    @Test fun rejectsPrivateAndAllowsNormalVideo() {
        assertFalse(YouTubeVideoFilter.isAllowed(120, "private", false, "none"))
        assertTrue(YouTubeVideoFilter.isAllowed(120, "public", false, "none"))
    }
}
