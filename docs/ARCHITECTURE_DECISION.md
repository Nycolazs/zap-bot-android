# Architecture Decision

This app uses a foreground service because Android does not allow a reliable permanent background bot process without a visible, user-controlled notification. The service starts foreground immediately, owns a `SupervisorJob`, and releases all runtime resources when stopped.

The bot can run as continuously as Android allows by combining a foreground service, a persistent notification, boot restart controlled by user settings, battery optimization guidance, and persisted jobs in Room. It never hides the notification.

WhatsApp is isolated behind `WhatsAppClient`. The included `FakeWhatsAppClient` supports local testing. `QrBridgeWhatsAppClient` is a skeleton for a replaceable WhatsApp Web bridge; it explicitly does not claim official QR support. Official WhatsApp Business Cloud API normally needs webhooks and a server, so it is not implemented as an on-device-only QR login.

Concurrency is handled with coroutines, a service-owned scope, a queued `DownloadQueueManager`, global concurrency limits, and per-chat active job checks. Each job keeps chat id, message id, video id, media type, output path, status, and progress.

Search sessions are stored by `chatId` in Room with a 30 minute expiration. Selection commands always read the session for the requesting chat, preventing cross-user state mixing.

Downloads are written under app-private cache folders in `cache/bot_jobs/{jobId}`. Successful sends delete the job folder when configured. Failed partial downloads are cleaned while keeping job metadata and logs.

Limitations: QR-based WhatsApp Web login on Android is unofficial and not reliably implemented in pure Kotlin. The project therefore ships the app, bot engine, fake client, and a bridge adapter contract for a real local companion implementation.
