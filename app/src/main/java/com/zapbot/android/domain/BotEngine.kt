package com.zapbot.android.domain

import com.zapbot.android.data.SearchSessionRepository
import com.zapbot.android.data.SelectionResult
import com.zapbot.android.database.DownloadJobDao
import com.zapbot.android.database.DownloadJobEntity
import com.zapbot.android.logging.BotLogger
import com.zapbot.android.queue.DownloadQueueManager
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
    private val logger: BotLogger
) {
    private val alertScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    suspend fun handle(message: IncomingWhatsAppMessage) {
        when (val command = parser.parse(message.text)) {
            BotCommand.Help -> whatsapp.sendText(message.chatId, helpText(), message.id)
            is BotCommand.Search -> search(message, command.query)
            is BotCommand.DownloadVideo -> enqueue(message, command.index, DownloadType.VIDEO)
            is BotCommand.DownloadAudio -> enqueue(message, command.index, DownloadType.AUDIO)
            BotCommand.Status -> status(message)
            BotCommand.Cancel -> cancel(message)
            is BotCommand.Invalid -> whatsapp.sendText(message.chatId, command.reason, message.id)
            BotCommand.Unknown -> Unit
        }
    }

    private suspend fun search(message: IncomingWhatsAppMessage, query: String) {
        try {
            alert("🔎 *Pesquisa recebida*\n\n_Chat:_ ${message.chatId}\n_Busca:_ $query")
            val results = youtube.searchVideos(query, SEARCH_RESULT_LIMIT)
            if (results.isEmpty()) {
                whatsapp.sendText(
                    message.chatId,
                    "🔎 *Não encontrei resultados disponíveis*\n\nTente uma busca mais específica, por exemplo:\n_/abertura pokemon_",
                    message.id
                )
                return
            }
            val sentMessageId = whatsapp.sendText(message.chatId, formatSearchResults(query, results), message.id)
            sessions.save(message.chatId, query, results, sentMessageId)
            alert("✅ *Pesquisa enviada*\n\n_Busca:_ $query\n_Resultados:_ ${results.size}")
            logger.info("BotEngine", "Search completed for chat ${message.chatId}")
        } catch (t: Throwable) {
            logger.error("BotEngine", "Search failed", t)
            alert("🚨 *Erro na pesquisa*\n\n_Busca:_ $query\n_Motivo:_ ${safeError(t)}")
            whatsapp.sendText(
                message.chatId,
                "⚠️ *Não consegui pesquisar agora*\n\n_Motivo:_ ${safeError(t)}\n\nTente novamente em instantes.",
                message.id
            )
        }
    }

    private suspend fun enqueue(message: IncomingWhatsAppMessage, index: Int, type: DownloadType) {
        val selected = selectForDownload(message, index)
        when (selected) {
            SelectionResult.Expired -> whatsapp.sendText(
                message.chatId,
                if (message.quotedMessageId.isNullOrBlank()) {
                    "⌛ *Sua pesquisa expirou*\n\nEnvie uma nova busca começando com */*.\n\n_Exemplo:_ */música relaxante*"
                } else {
                    "⌛ *Não encontrei a pesquisa dessa mensagem respondida*\n\nResponda diretamente a uma lista de resultados enviada pelo bot, ou faça uma nova busca."
                },
                message.id
            )
            is SelectionResult.InvalidIndex -> whatsapp.sendText(
                message.chatId,
                "🔢 *Resultado inválido*\n\nEscolha um número entre *1* e *${selected.available}*.\n\n_Exemplo:_ */v1* ou */a1*",
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
                    downloadStartedText(selected.video, type),
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

    private suspend fun status(message: IncomingWhatsAppMessage) {
        val job = jobDao.activeForChat(message.chatId)
        val text = if (job == null) {
            "✅ *Tudo livre por aqui*\n\nNenhum download em andamento no momento."
        } else {
            """
            📦 *Status do download*

            _${statusLabel(job.status)}_ • *${job.progress}%*
            ${job.title}
            """.trimIndent()
        }
        whatsapp.sendText(message.chatId, text, message.id)
    }

    private suspend fun cancel(message: IncomingWhatsAppMessage) {
        val job = jobDao.activeForChat(message.chatId)
        if (job == null) {
            whatsapp.sendText(message.chatId, "✅ *Nada para cancelar*\n\nNão há nenhum download ativo no momento.", message.id)
        } else {
            queue.cancel(job.id)
            whatsapp.sendText(message.chatId, "🛑 *Download cancelado com sucesso.*", message.id)
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

    private fun helpText() = """
        🤖 *ZapTube Bot*

        *Como pesquisar*
        Envie */* junto com o que você quer buscar.

        _Exemplos:_
        */música de zelda*
        */abertura pokemon*

        *Como baixar*
        🎬 Vídeo: */v1*
        🎧 Áudio: */a1*

        */status*
        Mostra o andamento do download atual.

        */cancel*
        Cancela o download atual.
    """.trimIndent()

    private fun downloadStartedText(video: YouTubeVideoResult, type: DownloadType): String {
        val icon = if (type == DownloadType.VIDEO) "🎬" else "🎧"
        return """
            $icon *Download iniciado*

            *${video.title}*
            ⏱️ _Duração:_ ${video.durationText}
            🗓️ _Publicado:_ ${video.publishedText ?: "Não informado"}
            📺 _Canal:_ ${video.channel}

            _Assim que estiver pronto, eu envio aqui no WhatsApp._
        """.trimIndent()
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

    private companion object {
        const val SEARCH_RESULT_LIMIT = 8
        const val ALERT_GROUP_NAME = "Alerta Music Bot"
    }
}
