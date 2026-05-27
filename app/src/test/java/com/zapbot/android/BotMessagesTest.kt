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

    @Test fun relativePublishedDatesUseBotLanguage() {
        val portugueseDateVideo = video.copy(publishedText = "há 6 anos")

        assertTrue(BotMessages("en").searchResults("test", listOf(portugueseDateVideo)).contains("_Published:_ 6 years ago"))
        assertTrue(BotMessages("es").searchResults("test", listOf(portugueseDateVideo)).contains("_Publicado:_ hace 6 años"))
        assertTrue(BotMessages("ru").searchResults("test", listOf(portugueseDateVideo)).contains("_Опубликовано:_ 6 лет назад"))
        assertTrue(BotMessages("pt").searchResults("test", listOf(portugueseDateVideo)).contains("_Publicado:_ há 6 anos"))
    }

    @Test fun welcomeUsesBotLanguageAndMentionsHelp() {
        assertTrue(BotMessages("en").welcome().contains("*/help*"))
        assertTrue(BotMessages("pt").welcome().contains("Bem-vindo ao Zappy"))
        assertTrue(BotMessages("es").welcome().contains("Bienvenido a Zappy"))
        assertTrue(BotMessages("ru").welcome().contains("Добро пожаловать в Zappy"))
    }

    @Test fun listedLanguagesHaveBotMessageTranslations() {
        val expected = mapOf(
            "en" to listOf("Tell me what you want", "To download a video", "Video ready", "Downloading"),
            "pt" to listOf("Me diga o que", "Para baixar um vídeo", "Vídeo pronto", "Baixando"),
            "es" to listOf("Dime qué quieres", "Para descargar un video", "Video listo", "Descargando"),
            "ru" to listOf("Напишите", "Чтобы скачать видео", "Видео готово", "Загрузка")
        )

        expected.forEach { (language, snippets) ->
            val messages = BotMessages(language)
            val combined = listOf(
                messages.missingSearchQuery(),
                messages.invalidDownloadCommand(DownloadType.VIDEO),
                messages.completedCaption(video, DownloadType.VIDEO, isPlaylist = false),
                messages.statusLabel(DownloadStatus.DOWNLOADING)
            ).joinToString("\n")

            snippets.forEach { snippet ->
                assertTrue("$language should contain $snippet", combined.contains(snippet))
            }
        }
    }
}
