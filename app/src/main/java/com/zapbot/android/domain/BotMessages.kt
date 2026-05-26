package com.zapbot.android.domain

class BotMessages(private val language: String) {
    fun searching(query: String) = when (language) {
        "pt" -> "🔎 Buscando *$query*..."
        "es" -> "🔎 Buscando *$query*..."
        "ru" -> "🔎 Ищу *$query*..."
        else -> "🔎 Searching for *$query*..."
    }

    fun help() = when (language) {
        "pt" -> """
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
        "es" -> """
            🤖 *ZapTube Bot*

            *Cómo buscar*
            Envía */* junto con lo que quieres buscar.

            _Ejemplos:_
            */música de zelda*
            */opening pokemon*

            *Cómo descargar*
            🎬 Video: */v1*
            🎧 Audio: */a1*

            */status*
            Muestra el progreso de la descarga actual.

            */cancel*
            Cancela la descarga actual.
        """.trimIndent()
        "ru" -> """
            🤖 *ZapTube Bot*

            *Как искать*
            Отправьте */* и текст поиска.

            _Примеры:_
            */zelda music*
            */pokemon opening*

            *Как скачать*
            🎬 Видео: */v1*
            🎧 Аудио: */a1*

            */status*
            Показывает текущую загрузку.

            */cancel*
            Отменяет текущую загрузку.
        """.trimIndent()
        else -> """
            🤖 *ZapTube Bot*

            *How to search*
            Send */* followed by what you want to search.

            _Examples:_
            */zelda music*
            */pokemon opening*

            *How to download*
            🎬 Video: */v1*
            🎧 Audio: */a1*

            */status*
            Shows the current download progress.

            */cancel*
            Cancels the current download.
        """.trimIndent()
    }

    fun emptySearch() = when (language) {
        "pt" -> "🔎 *Não encontrei resultados disponíveis*\n\nTente uma busca mais específica."
        "es" -> "🔎 *No encontré resultados disponibles*\n\nIntenta una búsqueda más específica."
        "ru" -> "🔎 *Результаты не найдены*\n\nПопробуйте более точный запрос."
        else -> "🔎 *No results found*\n\nTry a more specific search."
    }

    fun searchFailed(reason: String) = when (language) {
        "pt" -> "⚠️ *Não consegui pesquisar agora*\n\n_Motivo:_ $reason\n\nTente novamente em instantes."
        "es" -> "⚠️ *No pude buscar ahora*\n\n_Motivo:_ $reason\n\nInténtalo de nuevo en un momento."
        "ru" -> "⚠️ *Не удалось выполнить поиск*\n\n_Причина:_ $reason\n\nПопробуйте позже."
        else -> "⚠️ *I could not search right now*\n\n_Reason:_ $reason\n\nTry again shortly."
    }

    fun expired(quoted: Boolean) = when (language) {
        "pt" -> if (quoted) "⌛ *Não encontrei a pesquisa dessa mensagem respondida*\n\nResponda diretamente a uma lista de resultados enviada pelo bot, ou faça uma nova busca." else "⌛ *Sua pesquisa expirou*\n\nEnvie uma nova busca começando com */*."
        "es" -> if (quoted) "⌛ *No encontré la búsqueda de ese mensaje respondido*\n\nResponde a una lista enviada por el bot o haz una nueva búsqueda." else "⌛ *Tu búsqueda expiró*\n\nEnvía una nueva búsqueda empezando con */*."
        "ru" -> if (quoted) "⌛ *Не нашел поиск из отвеченного сообщения*\n\nОтветьте на список результатов от бота или сделайте новый поиск." else "⌛ *Поиск истек*\n\nОтправьте новый поиск, начиная с */*."
        else -> if (quoted) "⌛ *I could not find that replied search*\n\nReply to a results list from the bot or start a new search." else "⌛ *Your search expired*\n\nStart a new search with */*."
    }

    fun invalidIndex(available: Int) = when (language) {
        "pt" -> "🔢 *Resultado inválido*\n\nEscolha um número entre *1* e *$available*."
        "es" -> "🔢 *Resultado inválido*\n\nElige un número entre *1* y *$available*."
        "ru" -> "🔢 *Неверный результат*\n\nВыберите число от *1* до *$available*."
        else -> "🔢 *Invalid result*\n\nChoose a number between *1* and *$available*."
    }

    fun downloadStarted(video: YouTubeVideoResult, type: DownloadType): String {
        val icon = if (type == DownloadType.VIDEO) "🎬" else "🎧"
        val title = when (language) {
            "pt" -> "Download iniciado"
            "es" -> "Descarga iniciada"
            "ru" -> "Загрузка началась"
            else -> "Download started"
        }
        return "$icon *$title*\n\n*${video.title}*\n⏱️ ${video.durationText}\n📺 ${video.channel}"
    }

    fun statusIdle() = when (language) {
        "pt" -> "✅ *Tudo livre por aqui*\n\nNenhum download em andamento no momento."
        "es" -> "✅ *Todo libre*\n\nNo hay descargas activas ahora."
        "ru" -> "✅ *Все свободно*\n\nСейчас нет активных загрузок."
        else -> "✅ *All clear*\n\nNo active downloads right now."
    }

    fun statusActive(status: String, progress: Int, title: String) =
        "📦 *Status*\n\n_${status}_ • *$progress%*\n$title"

    fun cancelled() = when (language) {
        "pt" -> "🛑 *Download cancelado com sucesso.*"
        "es" -> "🛑 *Descarga cancelada.*"
        "ru" -> "🛑 *Загрузка отменена.*"
        else -> "🛑 *Download cancelled.*"
    }

    fun nothingToCancel() = when (language) {
        "pt" -> "✅ *Nada para cancelar*\n\nNão há nenhum download ativo no momento."
        "es" -> "✅ *Nada que cancelar*\n\nNo hay descargas activas ahora."
        "ru" -> "✅ *Нечего отменять*\n\nСейчас нет активных загрузок."
        else -> "✅ *Nothing to cancel*\n\nThere is no active download right now."
    }

    fun sendFailed(reason: String) = when (language) {
        "pt" -> "⚠️ *Não consegui concluir o envio*\n\n_Motivo:_ $reason\n\nVocê pode tentar outro resultado da lista ou baixar somente o áudio com */a1*."
        "es" -> "⚠️ *No pude completar el envío*\n\n_Motivo:_ $reason\n\nPuedes intentar otro resultado o descargar solo el audio con */a1*."
        "ru" -> "⚠️ *Не удалось завершить отправку*\n\n_Причина:_ $reason\n\nПопробуйте другой результат или скачайте только аудио через */a1*."
        else -> "⚠️ *I could not finish sending*\n\n_Reason:_ $reason\n\nTry another result or download audio only with */a1*."
    }
}
