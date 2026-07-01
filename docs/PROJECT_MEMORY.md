# Zappy Project Memory

Last studied: 2026-07-01

## What The Project Does

Zappy is a native Android WhatsApp-linked media bot. It connects as a WhatsApp linked device through the gomobile `whatsmeow` bridge, receives private chat slash commands, searches YouTube, downloads YouTube/Instagram/TikTok media with `youtubedl-android` and FFmpeg, and sends media or stickers back to WhatsApp.

## Active Architecture

- Android app entrypoint: `ZapBotApplication` creates `AppContainer`.
- Active WhatsApp client: `WhatsmeowWhatsAppClient`, backed by `app/libs/whatsmeowbridge.aar`.
- Active bridge source: `mobile-whatsapp/whatsmeowbridge/bridge.go`.
- Legacy HTTP/Node bridge: `whatsapp-bridge/server.js`; currently text-only for outgoing messages and not the main production media path.
- Runtime owner: `BotForegroundService`.
- Message handling: `BotForegroundService` collects `WhatsAppClient.incomingMessages` and calls `BotEngine.handle`.
- Command parsing: `BotCommandParser`.
- Search/session/job logic: `BotEngine`, `SearchSessionRepository`, Room DAOs.
- Download execution: `DownloadQueueManager` calls `YtDlpMediaDownloader`.
- Media sending: `WhatsmeowWhatsAppClient.sendMedia` calls Go bridge `SendMedia`.
- Sticker conversion: `StickerHelper` converts images to WebP before Go bridge `SendSticker`.

## Versioning And GitHub Release Flow

- Current version is defined in `app/build.gradle.kts`:
  - `versionCode = 12`
  - `versionName = "1.4.7"`
- README and release notes duplicate the current public version and should be updated when bumping versions.
- No `.github/workflows` release automation was found in this checkout.
- Git remote is `origin https://github.com/Nycolazs/Zappy.git`; current branch observed as `main`.
- In-app update checking uses GitHub Releases API in `UpdateChecker`:
  - `https://api.github.com/repos/Nycolazs/zap-bot-android/releases/latest`
  - It finds the latest release APK asset, downloads it to cache, then opens Android installer with `FileProvider`.
- Build command documented by the project: `./gradlew test assembleDebug`.

## Critical WhatsApp Safety Decisions

- Zappy must never respond in groups, status, broadcasts, or newsletters.
- Private chat policy lives in `WhatsAppChatPolicy`.
- Defense-in-depth points:
  - `BotEngine.handle` ignores non-private chats before welcome/sticker/parser.
  - `WhatsmeowWhatsAppClient` ignores non-private incoming bridge events.
  - `WhatsmeowWhatsAppClient` refuses text/media/sticker sends to non-private chat IDs.
  - Go bridge source refuses sends to non-private JIDs and ignores non-private incoming messages.
  - Legacy Node bridge filters incoming and outgoing text.
- Operational WhatsApp group alerts were removed. The previous `Alerta Zappy` behavior must not return.

## Runtime Diagnostics

- The Home screen includes a `What is happening` diagnostics card sourced from recent `Service`, `WhatsApp`, `Queue`, `BotEngine`, and `BootReceiver` logs.
- Start/stop actions are logged through `MainViewModel`.
- `BotForegroundService` logs blocked starts, duplicate starts, service start/stop, WhatsApp bridge start failures, and WhatsApp connection-state transitions.
- The Logs screen includes `WhatsApp` and `BootReceiver` tags so session loss, pairing-required states, and bridge errors are visible from inside the app.

## Media Reliability Decisions

- The old 1.5 GB video limit was too high for reliable WhatsApp video sending.
- Video upload limit is now 15 MB in both downloader selection and queue validation to avoid the old packaged bridge fallback that could turn oversized videos into generic documents.
- The Go bridge no longer falls back from video upload to document upload with `application/octet-stream`; that fallback caused generic `.bin` files.
- Kotlin media sending rejects unknown `application/octet-stream` media before calling the bridge.
- `YtDlpMediaDownloader` now applies stronger retries/timeouts, refreshes yt-dlp once per day when supported by the embedded library, and recodes Instagram/TikTok output to MP4.

## Sticker Metadata

Created stickers include WhatsApp WebP EXIF metadata:

`Sticker created with Zappy BOT 🤖 by @nycolazs`

The metadata is written in `StickerHelper` as sticker pack name, with publisher `@nycolazs`.

## Known Follow-Up Risk

The app packages `app/libs/whatsmeowbridge.aar`. Source changes in `mobile-whatsapp/whatsmeowbridge/bridge.go` require regenerating the AAR with gomobile before they affect a release APK. Kotlin-side guards still protect the active APK build path from group/status replies and `.bin` sends.
