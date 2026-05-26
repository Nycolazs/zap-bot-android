package com.zapbot.android.domain

import com.zapbot.android.data.SearchSessionRepository
import com.zapbot.android.data.SelectionResult
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
    private val jobDao: DownloadJobDao,
    private val queue: DownloadQueueManager,
    private val whatsapp: WhatsAppClient,
    private val settings: SettingsRepository,
    private val logger: BotLogger
) {
    private val alertScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun handle(message: IncomingWhatsAppMessage) {
        val text = BotMessages(settings.get().botLanguage)
        when (val command = parser.parse(message.text)) {
            BotCommand.Help -> whatsapp.sendText(message.chatId, text.help(), message.id)
            is BotCommand.Search -> search(message, command.query, text)
            is BotCommand.DownloadVideo -> enqueue(message, command.index, DownloadType.VIDEO, text)
            is BotCommand.DownloadAudio -> enqueue(message, command.index, DownloadType.AUDIO, text)
            BotCommand.Status -> status(message, text)
            BotCommand.Cancel -> cancel(message, text)
            is BotCommand.Invalid -> whatsapp.sendText(message.chatId, localizedInvalid(command.reason, text), message.id)
            BotCommand.Unknown -> Unit
        }
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
            val sentMessageId = whatsapp.sendText(message.chatId, formatSearchResults(query, results), message.id)
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
            text.statusActive(statusLabel(job.status), job.progress, job.title)
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

    private fun formatSearchResults(query: String, results: List<YouTubeVideoResult>): String = buildString {
        appendLine("🔎 *Resultados encontrados*")
        appendLine("_Pesquisa:_ $query")
        appendLine()
        results.forEachIndexed { index, video ->
            val number = index + 1
            appendLine("*$number. ${video.title}*")
            appendLine("⏱️ _Duração:_ ${video.durationText}")
            appendLine("🗓️ _Publicado:_ ${video.publishedText ?: "Não informado"}")
            appendLine("📺 _Canal:_ ${video.channel}")
            appendLine()
        }
        appendLine("✨ *Como baixar*")
        appendLine("🎬 Vídeo: envie */v1*, */v2*, */v3*...")
        appendLine("🎧 Áudio: envie */a1*, */a2*, */a3*...")
        appendLine()
        appendLine("_Dica:_ se você responder esta mensagem com */v1* ou */a1*, eu uso esta pesquisa, mesmo que exista uma pesquisa mais recente.")
    }

    private fun quotedSearchQuery(text: String?): String? {
        if (text.isNullOrBlank()) return null
        return text.lineSequence()
            .firstOrNull { it.startsWith("_Pesquisa:_") }
            ?.substringAfter("_Pesquisa:_")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun statusLabel(status: DownloadStatus): String = when (status) {
        DownloadStatus.QUEUED -> "Na fila"
        DownloadStatus.DOWNLOADING -> "Baixando"
        DownloadStatus.PROCESSING -> "Processando"
        DownloadStatus.SENDING -> "Enviando"
        DownloadStatus.COMPLETED -> "Concluído"
        DownloadStatus.FAILED -> "Falhou"
        DownloadStatus.CANCELLED -> "Cancelado"
    }

    private fun safeError(t: Throwable): String =
        (t.message ?: t.javaClass.simpleName).replace(Regex("[\\r\\n]+"), " ").take(180)

    private fun alert(text: String) {
        alertScope.launch {
            runCatching { whatsapp.sendTextToGroupName(ALERT_GROUP_NAME, text) }
        }
    }

    private fun localizedInvalid(reason: String, text: BotMessages): String = when {
        reason.contains("/v1") -> text.invalidIndex(8)
        reason.contains("/a1") -> text.invalidIndex(8)
        else -> text.emptySearch()
    }

    private companion object {
        const val SEARCH_RESULT_LIMIT = 8
        const val ALERT_GROUP_NAME = "Alerta Music Bot"
    }
}

private fun IncomingWhatsAppMessage.senderLabel(): String =
    senderName?.takeIf { it.isNotBlank() } ?: chatId.toReadableChatId()

private fun String.toReadableChatId(): String =
    substringBefore("@")
        .filter { it.isDigit() || it == '+' }
        .takeIf { it.isNotBlank() }
        ?: "WhatsApp contact"
