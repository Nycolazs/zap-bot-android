package com.zapbot.android

import com.zapbot.android.domain.BotCommand
import com.zapbot.android.domain.BotCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BotCommandParserTest {
    private val parser = BotCommandParser()

    @Test fun parsesSearchCaseInsensitive() {
        assertEquals(BotCommand.Search("minecraft 3ds"), parser.parse("  /PESQUISA   minecraft 3ds "))
        assertEquals(BotCommand.Search("minecraft 3ds"), parser.parse("  /SEARCH   minecraft 3ds "))
    }

    @Test fun parsesSearchWithWhatsAppSpacingAndMarks() {
        assertEquals(BotCommand.Search("minecraft 3ds"), parser.parse("\u200E/pesquisa\u00A0minecraft 3ds"))
        assertEquals(BotCommand.Search("minecraft 3ds"), parser.parse("/pesquisar minecraft 3ds"))
        assertEquals(BotCommand.Search("minecraft 3ds"), parser.parse("/p minecraft 3ds"))
    }

    @Test fun parsesAnySlashTextAsSearch() {
        assertEquals(BotCommand.Search("musica de zelda"), parser.parse("/musica de zelda"))
        assertEquals(BotCommand.Search("abertura pokemon"), parser.parse("  /abertura pokemon  "))
    }

    @Test fun rejectsSearchWithoutQuery() {
        assertTrue(parser.parse("/pesquisa") is BotCommand.Invalid)
        assertEquals(BotCommand.Unknown, parser.parse("/"))
    }

    @Test fun parsesDownloadCommands() {
        assertEquals(BotCommand.DownloadVideo(2), parser.parse("/v 2"))
        assertEquals(BotCommand.DownloadVideo(2), parser.parse("/v2"))
        assertEquals(BotCommand.DownloadVideo(1), parser.parse("/v"))
        assertEquals(BotCommand.DownloadAudio(1), parser.parse("/a"))
        assertEquals(BotCommand.DownloadAudio(7), parser.parse("/A 7"))
        assertEquals(BotCommand.DownloadAudio(7), parser.parse("/a7"))
    }

    @Test fun parsesDirectYouTubeLinks() {
        assertEquals(BotCommand.DownloadAudioLink("https://youtu.be/xVwF_JUM3xM?si=test"), parser.parse("/a https://youtu.be/xVwF_JUM3xM?si=test"))
        assertEquals(BotCommand.DownloadVideoLink("https://www.youtube.com/watch?v=xVwF_JUM3xM"), parser.parse("/v https://www.youtube.com/watch?v=xVwF_JUM3xM"))
        assertEquals(BotCommand.DownloadAudioLink("https://www.youtube.com/playlist?list=PL1234567890"), parser.parse("/a https://www.youtube.com/playlist?list=PL1234567890"))
        assertEquals(BotCommand.DownloadVideoLink("https://www.youtube.com/playlist?list=PL1234567890"), parser.parse("/v https://www.youtube.com/playlist?list=PL1234567890"))
    }

    @Test fun parsesDirectInstagramAndTikTokVideoLinks() {
        assertEquals(BotCommand.DownloadVideoLink("https://www.instagram.com/reel/ABC123/"), parser.parse("/v https://www.instagram.com/reel/ABC123/"))
        assertEquals(BotCommand.DownloadVideoLink("https://www.instagram.com/p/ABC123/"), parser.parse("/v https://www.instagram.com/p/ABC123/"))
        assertEquals(BotCommand.DownloadVideoLink("https://www.tiktok.com/@user/video/1234567890"), parser.parse("/v https://www.tiktok.com/@user/video/1234567890"))
        assertEquals(BotCommand.DownloadVideoLink("https://vm.tiktok.com/ZMabc123/"), parser.parse("/v https://vm.tiktok.com/ZMabc123/"))
    }

    @Test fun rejectsInstagramAndTikTokAudioLinks() {
        assertEquals(BotCommand.Invalid("SOCIAL_AUDIO_NOT_SUPPORTED"), parser.parse("/a https://www.instagram.com/reel/ABC123/"))
        assertEquals(BotCommand.Invalid("SOCIAL_AUDIO_NOT_SUPPORTED"), parser.parse("/a https://www.tiktok.com/@user/video/1234567890"))
    }

    @Test fun parsesStickerCommands() {
        assertEquals(BotCommand.Sticker, parser.parse("/sticker"))
        assertEquals(BotCommand.Sticker, parser.parse("/figurinha"))
    }

    @Test fun rejectsBadIndex() {
        assertTrue(parser.parse("/v x") is BotCommand.Invalid)
        assertTrue(parser.parse("/a 0") is BotCommand.Invalid)
        assertTrue(parser.parse("/a https://example.com/video") is BotCommand.Invalid)
    }

    @Test fun ignoresRegularMessages() {
        assertEquals(BotCommand.Unknown, parser.parse("hey, are you coming today?"))
    }
}
