package com.zapbot.android.domain

import com.zapbot.android.data.SearchSessionRepository
import com.zapbot.android.data.SelectionResult
import com.zapbot.android.database.ChatWelcomeDao
import com.zapbot.android.database.ChatWelcomeEntity
import com.zapbot.android.database.DownloadJobDao
import com.zapbot.android.database.DownloadJobEntity
import com.zapbot.android.logging.BotLogger
import com.zapbot.android.queue.DownloadQueueManager
import com.zapbot.android.settings.SettingsRepository
import com.zapbot.android.whatsapp.WhatsAppClient
import com.zapbot.android.youtube.YouTubeSearchClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BotEngine(
    private val parser: BotCommandParser,
    private val youtube: YouTubeSearchClient,
    private val sessions: SearchSessionRepository,
    private val welcomes: ChatWelcomeDao,
    private val jobDao: DownloadJobDao,
    private val queue: DownloadQueueManager,
    private val whatsapp: WhatsAppClient,
    private val settings: SettingsRepository,
    private val logger: BotLogger
) {
    private val alertScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun handle(message: IncomingWhatsAppMessage) {
        val currentSettings = settings.get()
        if (message.isBlacklisted(currentSettings.blacklistedNumbers)) {
            logger.info("BotEngine", "Ignored blacklisted sender ${message.senderLabel()}")
            return
        }
        val text = BotMessages(currentSettings.botLanguage)
        sendWelcomeIfNeeded(message, text)
        if (message.hasIncomingImage() && !message.isGroupChat()) {
            sendSticker(message, text)
            return
        }
        when (val command = parser.parse(message.text)) {
            BotCommand.Help -> whatsapp.sendText(message.chatId, text.help(), message.id)
            is BotCommand.Search -> search(message, command.query, text)
            is BotCommand.DownloadVideo -> enqueue(message, command.index, DownloadType.VIDEO, text)
            is BotCommand.DownloadAudio -> enqueue(message, command.index, DownloadType.AUDIO, text)
            is BotCommand.DownloadVideoLink -> enqueueLink(message, command.url, DownloadType.VIDEO, text)
            is BotCommand.DownloadAudioLink -> enqueueLink(message, command.url, DownloadType.AUDIO, text)
            BotCommand.Sticker -> sendSticker(message, text)
            BotCommand.Status -> status(message, text)
            BotCommand.Cancel -> cancel(message, text)
            is BotCommand.Invalid -> whatsapp.sendText(message.chatId, localizedInvalid(command.reason, text), message.id)
            BotCommand.Unknown -> Unit
        }
    }

    private suspend fun sendWelcomeIfNeeded(message: IncomingWhatsAppMessage, text: BotMessages) {
        if (welcomes.hasWelcomed(message.chatId)) return
        if (welcomes.insert(ChatWelcomeEntity(message.chatId)) == -1L) return
        whatsapp.sendText(message.chatId, text.welcome(), message.id)
    }

    private suspend fun search(message: IncomingWhatsAppMessage, query: String, text: BotMessages) {
        try {
            runCatching {
                whatsapp.sendText(message.chatId, text.searching(query), message.id)
            }
            alert("🔎 *Pesquisa recebida*\n\n_Chat:_ ${message.chatId}\n_Busca:_ $query")
            val results = youtube.searchVideos(query, SEARCH_RESULT_LIMIT)
            if (results.isEmpty()) {
                whatsapp.sendText(
                    message.chatId,
                    text.emptySearch(),
                    message.id
                )
                return
            }
            val sentMessageId = whatsapp.sendText(message.chatId, text.searchResults(query, results), message.id)
            sessions.save(message.chatId, query, results, sentMessageId)
            alert("✅ *Pesquisa enviada*\n\n_Busca:_ $query\n_Resultados:_ ${results.size}")
            logger.info("BotEngine", "Search completed for ${message.senderLabel()}")
        } catch (t: Throwable) {
            logger.error("BotEngine", "Search failed", t)
            alert("🚨 *Erro na pesquisa*\n\n_Busca:_ $query\n_Motivo:_ ${safeError(t)}")
            whatsapp.sendText(
                message.chatId,
                text.searchFailed(safeError(t)),
                message.id
            )
        }
    }

    private suspend fun enqueue(message: IncomingWhatsAppMessage, index: Int, type: DownloadType, text: BotMessages) {
        val selected = selectForDownload(message, index)
        when (selected) {
            SelectionResult.Expired -> whatsapp.sendText(
                message.chatId,
                text.expired(!message.quotedMessageId.isNullOrBlank()),
                message.id
            )
            is SelectionResult.InvalidIndex -> whatsapp.sendText(
                message.chatId,
                text.invalidIndex(selected.available),
                message.id
            )
            is SelectionResult.Selected -> {
                val jobId = jobDao.insert(
                    DownloadJobEntity(
                        chatId = message.chatId,
                        messageId = message.id,
                        youtubeVideoId = selected.video.videoId,
                        title = selected.video.title,
                        type = type,
                        status = DownloadStatus.QUEUED
                    )
                )
                whatsapp.sendText(
                    message.chatId,
                    text.downloadStarted(selected.video, type),
                    message.id
                )
                alert("⬇️ *Download enfileirado*\n\n_Tipo:_ ${if (type == DownloadType.VIDEO) "Vídeo" else "Áudio"}\n_Título:_ ${selected.video.title}\n_Canal:_ ${selected.video.channel}")
                queue.enqueue(DownloadRequest(jobId, message.chatId, message.id, selected.video, type))
            }
        }
    }

    private suspend fun enqueueLink(message: IncomingWhatsAppMessage, url: String, type: DownloadType, text: BotMessages) {
        val parsed = MediaUrlParser.parse(url)
        if (parsed == null) {
            whatsapp.sendText(message.chatId, text.invalidYouTubeLink(), message.id)
            return
        }
        if (!parsed.supportsAudio && type == DownloadType.AUDIO) {
            whatsapp.sendText(message.chatId, text.socialAudioNotSupported(), message.id)
            return
        }
        if (parsed.kind == MediaUrl.Kind.PLAYLIST && type == DownloadType.VIDEO) {
            whatsapp.sendText(message.chatId, text.playlistVideoNotSupported(), message.id)
            return
        }

        val isPlaylist = parsed.kind == MediaUrl.Kind.PLAYLIST
        val title = text.directLinkTitle(parsed.source, isPlaylist)
        val channel = when (parsed.source) {
            MediaUrl.Source.YOUTUBE -> "YouTube"
            MediaUrl.Source.INSTAGRAM -> "Instagram"
            MediaUrl.Source.TIKTOK -> "TikTok"
        }
        val video = YouTubeVideoResult(
            title = title,
            channel = channel,
            videoId = if (isPlaylist) "playlist" else url.substringAfterLast("/").substringBefore("?").take(32).ifBlank { "direct" },
            url = url,
            durationSeconds = 0,
            thumbnailUrl = null
        )
        val jobId = jobDao.insert(
            DownloadJobEntity(
                chatId = message.chatId,
                messageId = message.id,
                youtubeVideoId = video.videoId,
                title = title,
                type = type,
                status = DownloadStatus.QUEUED
            )
        )
        whatsapp.sendText(message.chatId, text.downloadStarted(video, type), message.id)
        alert("⬇️ *Download por link enfileirado*\n\n_Tipo:_ ${if (type == DownloadType.VIDEO) "Vídeo" else if (isPlaylist) "Playlist áudio" else "Áudio"}\n_Link:_ $url")
        queue.enqueue(DownloadRequest(jobId, message.chatId, message.id, video, type, sourceUrl = url, isPlaylist = isPlaylist))
    }

    private suspend fun selectForDownload(message: IncomingWhatsAppMessage, index: Int): SelectionResult {
        val selected = sessions.select(message.chatId, index, message.quotedMessageId)
        if (selected !is SelectionResult.Expired || message.quotedMessageId.isNullOrBlank()) {
            return selected
        }
        val query = quotedSearchQuery(message.quotedText) ?: return selected
        val results = youtube.searchVideos(query, SEARCH_RESULT_LIMIT)
        sessions.saveQuoted(message.chatId, message.quotedMessageId, query, results)
        return sessions.select(message.chatId, index, message.quotedMessageId)
    }

    private suspend fun status(message: IncomingWhatsAppMessage, text: BotMessages) {
        val job = jobDao.activeForChat(message.chatId)
        val response = if (job == null) {
            text.statusIdle()
        } else {
            text.statusActive(text.statusLabel(job.status), job.progress, job.title)
        }
        whatsapp.sendText(message.chatId, response, message.id)
    }

    private suspend fun cancel(message: IncomingWhatsAppMessage, text: BotMessages) {
        val job = jobDao.activeForChat(message.chatId)
        if (job == null) {
            whatsapp.sendText(message.chatId, text.nothingToCancel(), message.id)
        } else {
            queue.cancel(job.id)
            whatsapp.sendText(message.chatId, text.cancelled(), message.id)
        }
    }

    private suspend fun sendSticker(message: IncomingWhatsAppMessage, text: BotMessages) {
        if (message.isGroupChat()) return
        val media = listOfNotNull(message.media, message.quotedMedia)
            .firstOrNull { it.type == IncomingMediaType.IMAGE && it.file.exists() && it.file.length() > 0L }
        if (media == null) {
            whatsapp.sendText(message.chatId, text.stickerImageRequired(), message.id)
            return
        }
        runCatching {
            whatsapp.sendSticker(message.chatId, media.file, message.id)
        }.onFailure {
            logger.error("BotEngine", "Sticker conversion/send failed", it)
            whatsapp.sendText(message.chatId, text.stickerFailed(safeError(it)), message.id)
        }
    }

    private fun quotedSearchQuery(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val prefixes = listOf(
            "_Pesquisa:_",
            "_Search:_",
            "_Búsqueda:_",
            "_Busqueda:_",
            "_Поиск:_"
        )
        return text.lineSequence()
            .firstNotNullOfOrNull { line ->
                prefixes.firstOrNull { line.startsWith(it) }?.let { line.substringAfter(it) }
            }
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun safeError(t: Throwable): String =
        (t.message ?: t.javaClass.simpleName).replace(Regex("[\\r\\n]+"), " ").take(180)

    private fun alert(text: String) {
        alertScope.launch {
            runCatching { whatsapp.sendTextToGroupName(ALERT_GROUP_NAME, text) }
        }
    }

    private fun localizedInvalid(reason: String, text: BotMessages): String = when {
        reason == "MISSING_SEARCH_QUERY" -> text.missingSearchQuery()
        reason == "INVALID_YOUTUBE_LINK" -> text.invalidYouTubeLink()
        reason == "SOCIAL_AUDIO_NOT_SUPPORTED" -> text.socialAudioNotSupported()
        reason == "INVALID_VIDEO_INDEX" -> text.invalidDownloadCommand(DownloadType.VIDEO)
        reason == "INVALID_AUDIO_INDEX" -> text.invalidDownloadCommand(DownloadType.AUDIO)
        else -> text.emptySearch()
    }

    private companion object {
        const val SEARCH_RESULT_LIMIT = 8
        const val ALERT_GROUP_NAME = "Alerta Zappy"
    }
}

private fun IncomingWhatsAppMessage.senderLabel(): String =
    senderName?.takeIf { it.isNotBlank() } ?: chatId.toReadableChatId()

private fun IncomingWhatsAppMessage.isBlacklisted(rawBlacklist: String): Boolean {
    val senderDigits = chatId.filter(Char::isDigit)
    if (senderDigits.isBlank()) return false
    return rawBlacklist.lineSequence()
        .map { it.filter(Char::isDigit) }
        .filter { it.isNotBlank() }
        .any { blocked -> senderDigits == blocked || senderDigits.endsWith(blocked) || blocked.endsWith(senderDigits) }
}

private fun IncomingWhatsAppMessage.hasIncomingImage(): Boolean =
    media?.let { it.type == IncomingMediaType.IMAGE && it.file.exists() && it.file.length() > 0L } == true

private fun IncomingWhatsAppMessage.isGroupChat(): Boolean =
    chatId.substringBefore(':').endsWith("@g.us", ignoreCase = true)

private fun String.toReadableChatId(): String =
    substringBefore("@")
        .filter { it.isDigit() || it == '+' }
        .takeIf { it.isNotBlank() }
        ?: "WhatsApp contact"
