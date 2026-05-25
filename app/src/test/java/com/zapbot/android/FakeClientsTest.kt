package com.zapbot.android

import com.zapbot.android.downloader.FakeMediaDownloader
import com.zapbot.android.youtube.FakeYouTubeSearchClient
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FakeClientsTest {
    @Test fun fakeYoutubeReturnsPerQueryResults() = runTest {
        val results = FakeYouTubeSearchClient().searchVideos("teste", 8)
        assertEquals(8, results.size)
        assertTrue(results.all { it.title.contains("teste") })
    }

    @Test fun fakeDownloaderCreatesAFile() = runTest {
        val video = FakeYouTubeSearchClient().searchVideos("audio", 1).first()
        val dir = File(System.getProperty("java.io.tmpdir"), "zapbot-test-${System.nanoTime()}")
        val downloader = FakeMediaDownloader()
        downloader.downloadAudio(1L, video, dir, "128k").toList()
        assertTrue(downloader.resultFile(dir).file.exists())
        dir.deleteRecursively()
    }
}
