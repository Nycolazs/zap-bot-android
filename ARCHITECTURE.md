# Architecture

The app follows a simple layered structure:

- `ui`: Compose screens and ViewModel.
- `service`: Foreground Service and BootReceiver.
- `domain`: models, command parser, and bot engine.
- `database`: Room DAOs and entities.
- `data`: repositories and file cleanup.
- `whatsapp`: `WhatsAppClient` contract, fake client, bridge clients, and the whatsmeow client.
- `youtube`: search contract, fast web client, and YouTube Data API v3 client.
- `downloader`: downloader contract, fake implementation, and youtubedl-android implementation.
- `queue`: job queue and concurrency control.
- `notifications`: persistent Android notification.

`BotEngine` does not depend on the Android UI. It receives messages, interprets commands, stores per-chat sessions, creates jobs, and sends replies through `WhatsAppClient`.

`BotForegroundService` starts in the foreground immediately, starts the WhatsApp client, observes incoming messages, starts the download queue, and updates notifications with connection status and active downloads.

Room storage prevents essential state from being lost. On startup, interrupted jobs are marked as `FAILED` with a safe user-facing message, avoiding stuck `DOWNLOADING` or `SENDING` states after a process shutdown.
