package com.zapbot.android

import com.zapbot.android.domain.BotMessages
import com.zapbot.android.domain.DownloadStatus
import com.zapbot.android.domain.DownloadType
import com.zapbot.android.domain.YouTubeVideoResult
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BotMessagesTest {
    private val video = YouTubeVideoResult(
        title = "Test Song",
        channel = "Test Channel",
        videoId = "abc123",
        url = "https://youtu.be/abc123",
        durationSeconds = 120,
        thumbnailUrl = null,
        publishedText = null
    )

    @Test fun englishSearchResultsUseBotLanguage() {
        val message = BotMessages("en").searchResults("test", listOf(video))

        assertTrue(message.contains("*Results found*"))
        assertTrue(message.contains("_Search:_ test"))
        assertTrue(message.contains("_Duration:_"))
        assertTrue(message.contains("_Published:_ Not available"))
        assertTrue(message.contains("Video: send */v1*"))
        assertFalse(message.contains("Resultados encontrados"))
        assertFalse(message.contains("_Pesquisa:_"))
    }

    @Test fun englishCompletedCaptionUsesBotLanguage() {
        val caption = BotMessages("en").completedCaption(video, DownloadType.AUDIO, isPlaylist = false)

        assertTrue(caption.contains("*Audio ready*"))
        assertTrue(caption.contains("_Duration:_"))
        assertTrue(caption.contains("_Published:_ Not available"))
        assertFalse(caption.contains("Áudio pronto"))
        assertFalse(caption.contains("_Duração:_"))
    }

    @Test fun englishStatusLabelUsesBotLanguage() {
        assertTrue(BotMessages("en").statusLabel(DownloadStatus.DOWNLOADING) == "Downloading")
    }
}
