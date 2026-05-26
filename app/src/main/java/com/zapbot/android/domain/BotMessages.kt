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

    fun missingSearchQuery() = when (language) {
        "pt" -> "🔎 *Me diga o que você quer buscar*\n\n_Exemplo:_ */música relaxante*"
        "es" -> "🔎 *Dime qué quieres buscar*\n\n_Ejemplo:_ */música relajante*"
        "ru" -> "🔎 *Напишите, что нужно найти*\n\n_Пример:_ */relaxing music*"
        else -> "🔎 *Tell me what you want to search for*\n\n_Example:_ */relaxing music*"
    }

    fun invalidDownloadCommand(type: DownloadType) = when (language) {
        "pt" -> if (type == DownloadType.VIDEO) "🎬 *Para baixar um vídeo, use assim:*\n*/v1*" else "🎧 *Para baixar um áudio, use assim:*\n*/a1*"
        "es" -> if (type == DownloadType.VIDEO) "🎬 *Para descargar un video, usa:*\n*/v1*" else "🎧 *Para descargar audio, usa:*\n*/a1*"
        "ru" -> if (type == DownloadType.VIDEO) "🎬 *Чтобы скачать видео, используйте:*\n*/v1*" else "🎧 *Чтобы скачать аудио, используйте:*\n*/a1*"
        else -> if (type == DownloadType.VIDEO) "🎬 *To download a video, use:*\n*/v1*" else "🎧 *To download audio, use:*\n*/a1*"
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

    fun statusActive(status: String, progress: Int, title: String): String {
        val label = when (language) {
            "ru" -> "Статус"
            else -> "Status"
        }
        return "📦 *$label*\n\n_${status}_ • *$progress%*\n$title"
    }

    fun searchResults(query: String, results: List<YouTubeVideoResult>): String = buildString {
        when (language) {
            "pt" -> {
                appendLine("🔎 *Resultados encontrados*")
                appendLine("_Pesquisa:_ $query")
            }
            "es" -> {
                appendLine("🔎 *Resultados encontrados*")
                appendLine("_Búsqueda:_ $query")
            }
            "ru" -> {
                appendLine("🔎 *Результаты найдены*")
                appendLine("_Поиск:_ $query")
            }
            else -> {
                appendLine("🔎 *Results found*")
                appendLine("_Search:_ $query")
            }
        }
        appendLine()
        results.forEachIndexed { index, video ->
            val number = index + 1
            appendLine("*$number. ${video.title}*")
            appendLine("${durationLabel()} ${video.durationText}")
            appendLine("${publishedLabel()} ${video.publishedText ?: notAvailable()}")
            appendLine("${channelLabel()} ${video.channel}")
            appendLine()
        }
        appendLine(downloadInstructionsTitle())
        appendLine(videoInstruction())
        appendLine(audioInstruction())
        appendLine()
        appendLine(replyTip())
    }

    fun searchQueryLinePrefixes(): List<String> = when (language) {
        "pt" -> listOf("_Pesquisa:_")
        "es" -> listOf("_Búsqueda:_", "_Busqueda:_")
        "ru" -> listOf("_Поиск:_")
        else -> listOf("_Search:_")
    }

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

    fun completedCaption(video: YouTubeVideoResult, type: DownloadType, isPlaylist: Boolean): String {
        val icon = when {
            isPlaylist -> "🎵"
            type == DownloadType.VIDEO -> "🎬"
            else -> "🎧"
        }
        val label = when (language) {
            "pt" -> when {
                isPlaylist -> "Playlist pronta"
                type == DownloadType.VIDEO -> "Vídeo pronto"
                else -> "Áudio pronto"
            }
            "es" -> when {
                isPlaylist -> "Playlist lista"
                type == DownloadType.VIDEO -> "Video listo"
                else -> "Audio listo"
            }
            "ru" -> when {
                isPlaylist -> "Плейлист готов"
                type == DownloadType.VIDEO -> "Видео готово"
                else -> "Аудио готово"
            }
            else -> when {
                isPlaylist -> "Playlist ready"
                type == DownloadType.VIDEO -> "Video ready"
                else -> "Audio ready"
            }
        }
        return """
            $icon *$label*

            *${video.title}*
            ${durationLabel()} ${video.durationText}
            ${publishedLabel()} ${video.publishedText ?: notAvailable()}
            ${channelLabel()} ${video.channel}
        """.trimIndent()
    }

    fun statusLabel(status: DownloadStatus): String = when (language) {
        "pt" -> when (status) {
            DownloadStatus.QUEUED -> "Na fila"
            DownloadStatus.DOWNLOADING -> "Baixando"
            DownloadStatus.PROCESSING -> "Processando"
            DownloadStatus.SENDING -> "Enviando"
            DownloadStatus.COMPLETED -> "Concluído"
            DownloadStatus.FAILED -> "Falhou"
            DownloadStatus.CANCELLED -> "Cancelado"
        }
        "es" -> when (status) {
            DownloadStatus.QUEUED -> "En cola"
            DownloadStatus.DOWNLOADING -> "Descargando"
            DownloadStatus.PROCESSING -> "Procesando"
            DownloadStatus.SENDING -> "Enviando"
            DownloadStatus.COMPLETED -> "Completado"
            DownloadStatus.FAILED -> "Falló"
            DownloadStatus.CANCELLED -> "Cancelado"
        }
        "ru" -> when (status) {
            DownloadStatus.QUEUED -> "В очереди"
            DownloadStatus.DOWNLOADING -> "Загрузка"
            DownloadStatus.PROCESSING -> "Обработка"
            DownloadStatus.SENDING -> "Отправка"
            DownloadStatus.COMPLETED -> "Завершено"
            DownloadStatus.FAILED -> "Ошибка"
            DownloadStatus.CANCELLED -> "Отменено"
        }
        else -> when (status) {
            DownloadStatus.QUEUED -> "Queued"
            DownloadStatus.DOWNLOADING -> "Downloading"
            DownloadStatus.PROCESSING -> "Processing"
            DownloadStatus.SENDING -> "Sending"
            DownloadStatus.COMPLETED -> "Completed"
            DownloadStatus.FAILED -> "Failed"
            DownloadStatus.CANCELLED -> "Cancelled"
        }
    }

    fun videoTooLarge(actualSize: String, maxSize: String): String = when (language) {
        "pt" -> "O vídeo ficou acima do limite de envio do bot ($actualSize). O limite atual é $maxSize."
        "es" -> "El video superó el límite de envío del bot ($actualSize). El límite actual es $maxSize."
        "ru" -> "Видео превышает лимит отправки бота ($actualSize). Текущий лимит: $maxSize."
        else -> "The video is above the bot upload limit ($actualSize). Current limit is $maxSize."
    }

    fun incompatibleVideoFile(extension: String): String = when (language) {
        "pt" -> "O downloader gerou um arquivo que não é vídeo compatível: .$extension"
        "es" -> "El descargador generó un archivo que no es un video compatible: .$extension"
        "ru" -> "Загрузчик создал несовместимый видеофайл: .$extension"
        else -> "The downloader generated a file that is not a compatible video: .$extension"
    }

    fun incompatibleAudioFile(extension: String): String = when (language) {
        "pt" -> "O downloader gerou um arquivo de vídeo para um pedido de áudio: .$extension"
        "es" -> "El descargador generó un archivo de video para una solicitud de audio: .$extension"
        "ru" -> "Загрузчик создал видеофайл для аудиозапроса: .$extension"
        else -> "The downloader generated a video file for an audio request: .$extension"
    }

    fun incompatiblePlaylistFile(extension: String): String = when (language) {
        "pt" -> "A playlist deveria ser enviada como .zip, mas o arquivo gerado foi .$extension"
        "es" -> "La playlist debería enviarse como .zip, pero el archivo generado fue .$extension"
        "ru" -> "Плейлист должен отправляться как .zip, но создан файл .$extension"
        else -> "The playlist should be sent as .zip, but the generated file was .$extension"
    }

    fun downloaderEmptyFileFallback() = when (language) {
        "pt" -> "o downloader não gerou um arquivo válido"
        "es" -> "el descargador no generó un archivo válido"
        "ru" -> "загрузчик не создал корректный файл"
        else -> "the downloader did not generate a valid file"
    }

    private fun durationLabel() = when (language) {
        "pt" -> "⏱️ _Duração:_"
        "es" -> "⏱️ _Duración:_"
        "ru" -> "⏱️ _Длительность:_"
        else -> "⏱️ _Duration:_"
    }

    private fun publishedLabel() = when (language) {
        "pt" -> "🗓️ _Publicado:_"
        "es" -> "🗓️ _Publicado:_"
        "ru" -> "🗓️ _Опубликовано:_"
        else -> "🗓️ _Published:_"
    }

    private fun channelLabel() = when (language) {
        "pt" -> "📺 _Canal:_"
        "es" -> "📺 _Canal:_"
        "ru" -> "📺 _Канал:_"
        else -> "📺 _Channel:_"
    }

    private fun notAvailable() = when (language) {
        "pt" -> "Não informado"
        "es" -> "No informado"
        "ru" -> "Не указано"
        else -> "Not available"
    }

    private fun downloadInstructionsTitle() = when (language) {
        "pt" -> "✨ *Como baixar*"
        "es" -> "✨ *Cómo descargar*"
        "ru" -> "✨ *Как скачать*"
        else -> "✨ *How to download*"
    }

    private fun videoInstruction() = when (language) {
        "pt" -> "🎬 Vídeo: envie */v1*, */v2*, */v3*..."
        "es" -> "🎬 Video: envía */v1*, */v2*, */v3*..."
        "ru" -> "🎬 Видео: отправьте */v1*, */v2*, */v3*..."
        else -> "🎬 Video: send */v1*, */v2*, */v3*..."
    }

    private fun audioInstruction() = when (language) {
        "pt" -> "🎧 Áudio: envie */a1*, */a2*, */a3*..."
        "es" -> "🎧 Audio: envía */a1*, */a2*, */a3*..."
        "ru" -> "🎧 Аудио: отправьте */a1*, */a2*, */a3*..."
        else -> "🎧 Audio: send */a1*, */a2*, */a3*..."
    }

    private fun replyTip() = when (language) {
        "pt" -> "_Dica:_ se você responder esta mensagem com */v1* ou */a1*, eu uso esta pesquisa, mesmo que exista uma pesquisa mais recente."
        "es" -> "_Consejo:_ si respondes este mensaje con */v1* o */a1*, uso esta búsqueda aunque exista una más reciente."
        "ru" -> "_Совет:_ если ответить на это сообщение командой */v1* или */a1*, я использую этот поиск, даже если есть более новый."
        else -> "_Tip:_ if you reply to this message with */v1* or */a1*, I use this search even if there is a newer one."
    }
}
