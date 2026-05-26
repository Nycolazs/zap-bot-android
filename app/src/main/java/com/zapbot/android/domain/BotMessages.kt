package com.zapbot.android.domain

class BotMessages(language: String) {
    private val language = LanguageResolver.resolve(language)

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

            *Links diretos*
            🎧 */a https://youtu.be/exemplo* — baixa MP3
            🎬 */v https://youtu.be/exemplo* — baixa vídeo
            🎵 */a link-da-playlist* — envia os MP3 em um .zip

            ⚠️ Playlists funcionam apenas com */a*.

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

            *Enlaces directos*
            🎧 */a https://youtu.be/ejemplo* — descarga MP3
            🎬 */v https://youtu.be/ejemplo* — descarga video
            🎵 */a enlace-de-playlist* — envía los MP3 en un .zip

            ⚠️ Las playlists solo funcionan con */a*.

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

            *Прямые ссылки*
            🎧 */a https://youtu.be/example* — скачать MP3
            🎬 */v https://youtu.be/example* — скачать видео
            🎵 */a ссылка-на-плейлист* — отправить MP3 в .zip

            ⚠️ Плейлисты работают только с */a*.

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

            *Direct links*
            🎧 */a https://youtu.be/example* — download MP3
            🎬 */v https://youtu.be/example* — download video
            🎵 */a playlist-link* — send MP3 files in a .zip

            ⚠️ Playlists work only with */a*.

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

    fun invalidYouTubeLink() = when (language) {
        "pt" -> "🔗 *Link inválido*\n\nUse um link de vídeo ou playlist do YouTube.\n\n_Exemplos:_\n*/a https://youtu.be/exemplo*\n*/v https://youtu.be/exemplo*"
        "es" -> "🔗 *Enlace inválido*\n\nUsa un enlace de video o playlist de YouTube.\n\n_Ejemplos:_\n*/a https://youtu.be/ejemplo*\n*/v https://youtu.be/ejemplo*"
        "ru" -> "🔗 *Неверная ссылка*\n\nИспользуйте ссылку на видео или плейлист YouTube.\n\n_Примеры:_\n*/a https://youtu.be/example*\n*/v https://youtu.be/example*"
        else -> "🔗 *Invalid link*\n\nUse a YouTube video or playlist link.\n\n_Examples:_\n*/a https://youtu.be/example*\n*/v https://youtu.be/example*"
    }

    fun playlistVideoNotSupported() = when (language) {
        "pt" -> "🎵 *Playlists são suportadas apenas para áudio.*\n\nUse */a* com o link da playlist para receber os MP3 em um arquivo .zip."
        "es" -> "🎵 *Las playlists solo son compatibles con audio.*\n\nUsa */a* con el enlace de la playlist para recibir los MP3 en un archivo .zip."
        "ru" -> "🎵 *Плейлисты поддерживаются только для аудио.*\n\nИспользуйте */a* со ссылкой на плейлист, чтобы получить MP3 в .zip."
        else -> "🎵 *Playlists are supported for audio only.*\n\nUse */a* with the playlist link to receive the MP3 files in a .zip."
    }
}
