package com.zapbot.android.domain

class BotCommandParser {
    fun parse(input: String): BotCommand {
        val text = input.normalizedWhatsAppText()
        if (text.isBlank()) return BotCommand.Unknown
        val parts = text.split(commandSeparator, limit = 2)
        return when (parts.first().lowercase()) {
            "/help" -> BotCommand.Help
            "/status" -> BotCommand.Status
            "/cancel" -> BotCommand.Cancel
            "/pesquisa", "/pesquisar", "/p", "/search" -> {
                val query = parts.getOrNull(1)?.trim().orEmpty()
                if (query.isBlank()) BotCommand.Invalid("🔎 *Me diga o que você quer buscar*\n\n_Exemplo:_ */música relaxante*")
                else BotCommand.Search(query)
            }
            else -> parseCompactDownloadOrSlashSearch(text, parts.first())
        }
    }

    private fun parseCompactDownloadOrSlashSearch(text: String, command: String): BotCommand {
        val normalized = command.lowercase()
        return when {
            normalized == "/v" -> parseIndex(text.substringAfter(command, "").ifBlank { "1" }, true)
            normalized == "/a" -> parseIndex(text.substringAfter(command, "").ifBlank { "1" }, false)
            normalized.matches(Regex("^/v\\d+$")) -> parseIndex(normalized.drop(2), true)
            normalized.matches(Regex("^/a\\d+$")) -> parseIndex(normalized.drop(2), false)
            else -> parseSlashSearch(text)
        }
    }

    private fun parseIndex(raw: String?, video: Boolean): BotCommand {
        val index = raw?.trim()?.toIntOrNull()
        if (index == null || index < 1) {
            return BotCommand.Invalid(if (video) "🎬 *Para baixar um vídeo, use assim:*\n*/v1*" else "🎧 *Para baixar um áudio, use assim:*\n*/a1*")
        }
        return if (video) BotCommand.DownloadVideo(index) else BotCommand.DownloadAudio(index)
    }

    private fun parseSlashSearch(text: String): BotCommand {
        if (!text.startsWith("/") || text.length == 1) return BotCommand.Unknown
        val query = text.drop(1).trim()
        if (query.isBlank()) return BotCommand.Unknown
        return BotCommand.Search(query)
    }

    private fun String.normalizedWhatsAppText(): String =
        replace(invisibleMarks, "")
            .replace('\u00A0', ' ')
            .trim()

    private companion object {
        val commandSeparator = Regex("[\\s\\u00A0]+")
        val invisibleMarks = Regex("[\\u200B-\\u200F\\uFEFF]")
    }
}
