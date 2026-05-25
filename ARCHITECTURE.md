# Arquitetura

O app segue camadas simples:

- `ui`: telas Compose e ViewModel.
- `service`: Foreground Service e BootReceiver.
- `domain`: modelos, parser e motor do bot.
- `database`: Room DAOs e entidades.
- `data`: repositorios e limpeza de arquivos.
- `whatsapp`: contrato `WhatsAppClient`, fake e skeleton QR bridge.
- `youtube`: contrato e cliente YouTube Data API v3.
- `downloader`: contrato, fake e skeleton para downloader real.
- `queue`: fila de jobs e concorrencia.
- `notifications`: notificacao persistente.

O `BotEngine` nao depende da UI Android. Ele recebe mensagens, interpreta comandos, salva sessoes por chat, cria jobs e envia respostas pelo `WhatsAppClient`.

O `BotForegroundService` inicia imediatamente em foreground, sobe o cliente WhatsApp, observa mensagens, inicia a fila e atualiza notificacoes com status de conexao e downloads ativos.

O banco Room impede perda de estado essencial. Ao iniciar, a fila marca jobs interrompidos como `FAILED` com mensagem segura, evitando que estados `DOWNLOADING` ou `SENDING` fiquem presos apos encerramento do processo.
