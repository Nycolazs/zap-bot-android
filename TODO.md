# TODO realista

- Melhorar modo WhatsApp local por notificacoes, incluindo deduplicacao de mensagens agrupadas e deteccao de conversas silenciadas.
- Implementar bridge WhatsApp real opcional atras de `WhatsAppClient`.
- Exibir QR como bitmap quando o bridge real emitir dados QR.
- Implementar downloader real compativel com Android atras de `MediaDownloader`.
- Adicionar FFmpeg/transcoding e verificacao de tamanho por limite do WhatsApp.
- Validar em aparelho real o uso de `YouTubeApiClient` com chave configurada.
- Adicionar testes Room com banco em memoria para sessoes por chat, jobs e recuperacao de fila.
- Persistir status de runtime para estatisticas precisas apos reinicio.
