# Architecture Decision

This app uses a foreground service because Android does not allow a reliable permanent background bot process without a visible, user-controlled notification. The service starts foreground immediately, owns a `SupervisorJob`, and releases all runtime resources when stopped.

The bot can run as continuously as Android allows by combining a foreground service, a persistent notification, boot restart controlled by user settings, battery optimization guidance, and persisted jobs in Room. It never hides the notification.

WhatsApp is isolated behind `WhatsAppClient`. The included `FakeWhatsAppClient` supports local testing, bridge clients keep the integration replaceable, and `WhatsmeowWhatsAppClient` provides the current linked-device implementation packaged into the APK through gomobile. Official WhatsApp Business Cloud API normally needs webhooks and a server, so it is not implemented as an on-device-only linked-device login.

Concurrency is handled with coroutines, a service-owned scope, a queued `DownloadQueueManager`, global concurrency limits, and per-chat active job checks. Each job keeps chat id, message id, video id, media type, output path, status, and progress.

Search sessions are stored by `chatId` in Room with a 30 minute expiration. Selection commands always read the session for the requesting chat, preventing cross-user state mixing.

Downloads are written under app-private cache folders in `cache/bot_jobs/{jobId}`. Successful sends delete the job folder when configured. Failed partial downloads are cleaned while keeping job metadata and logs.

Limitations: linked-device WhatsApp integration for personal accounts is unofficial and can break if WhatsApp changes its web protocol. The app keeps WhatsApp behind an adapter boundary so the implementation can be replaced without rewriting the bot engine.
