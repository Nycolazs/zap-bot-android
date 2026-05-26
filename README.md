# 🤖 ZapTube Bot Android

ZapTube Bot is a native Android app that turns an Android phone into a WhatsApp-linked YouTube search and media delivery bot. It pairs with WhatsApp as a linked device, listens for slash commands, searches YouTube, downloads media on-device, and sends the result back to the chat.

The project is built with Kotlin, Jetpack Compose, Material 3, Room, youtubedl-android, FFmpeg, and a gomobile/whatsmeow bridge.

## ✨ Highlights

- 📱 Runs directly on Android as a foreground service.
- 🔗 Connects to WhatsApp through Linked Devices using `whatsmeow`.
- 🔎 Searches YouTube from WhatsApp commands.
- 🎬 Sends videos as video files instead of generic `.bin` attachments.
- 🎧 Sends audio using the best available YouTube audio format.
- ⚡ Uses wake and Wi-Fi locks while running for faster, more reliable processing.
- 🧵 Supports concurrent downloads with a configurable queue.
- 🌗 Includes light, dark, and system appearance modes.
- 🌍 Supports app and bot-message languages: English, Portuguese, Spanish, and Russian.
- 📊 Shows jobs, logs, uptime, status, and recent errors inside the app.
- 🔔 Uses a clean foreground notification with a stop action.

## 📸 App Screens

The app is organized into four main areas:

- **Home**: bot status, uptime, quick start/stop, and daily metrics.
- **Jobs**: recent downloads, progress, status, and errors.
- **Logs**: bot-only logs with an error filter.
- **Settings**: WhatsApp pairing, appearance, language, performance, notifications, quality, and cache controls.

## 📦 Latest Release

Download the latest APK from GitHub Releases:

👉 https://github.com/Nycolazs/zap-bot-android/releases

Current app version:

- `versionName`: `1.4.2`
- `versionCode`: `7`
- `minSdk`: `26`
- `targetSdk`: `35`

## 🚀 Quick Start

1. Download the APK from the latest release.
2. Install it on the Android phone that will run the bot.
3. Open **ZapTube Bot**.
4. Go to **Settings**.
5. Enter your WhatsApp number with country and area code.
6. Generate a Linked Devices code.
7. On your main WhatsApp phone, open:
   `WhatsApp > Linked devices > Link with phone number`
8. Enter the code shown in the app.
9. Return to Home and tap **Start bot**.

For best reliability, open Android battery settings for the app and disable battery optimization.

## 💬 WhatsApp Commands

| Command | What it does |
| --- | --- |
| `/help` | Shows bot help. |
| `/{search query}` | Searches YouTube. Example: `/zelda music`. |
| `/search zelda music` | Searches YouTube explicitly. |
| `/v1` | Downloads and sends video result 1. |
| `/a1` | Downloads and sends audio result 1. |
| `/v` | Downloads result 1, especially useful as a reply to a search message. |
| `/a` | Downloads audio result 1, especially useful as a reply. |
| `/status` | Shows the active download status for that chat. |
| `/cancel` | Cancels the active job for that chat. |

When replying to an older search result message with `/v1` or `/a1`, the bot uses the replied search results instead of the latest search in the chat.

## 🌍 Languages

The app supports two independent language settings:

- **App language**: changes the Android UI labels.
- **Bot message language**: changes the messages sent in WhatsApp.

Available languages:

- 🇺🇸 English
- 🇧🇷 Português
- 🇪🇸 Español
- 🇷🇺 Русский

## ⚙️ Settings

The Settings screen includes:

- WhatsApp Linked Devices pairing.
- App language and bot message language.
- Light, dark, and system theme modes.
- Start on boot.
- Notifications and detailed notifications.
- Battery settings shortcut.
- Concurrent download count.
- Video quality limit.
- Audio bitrate preference.
- WhatsApp cache/session clearing.

Settings are persisted with Room and survive app restarts.

## 🧠 Architecture

The codebase follows a simple layered structure:

| Layer | Responsibility |
| --- | --- |
| `ui` | Compose screens, theme, localized UI strings, and ViewModel. |
| `service` | Foreground service, boot receiver, runtime state, wake/Wi-Fi locks. |
| `domain` | Bot commands, models, parser, bot engine, localized bot messages. |
| `database` | Room entities, DAOs, converters, and migrations. |
| `settings` | Settings repository and normalization. |
| `whatsapp` | WhatsApp client contract and whatsmeow bridge implementation. |
| `youtube` | YouTube search clients. |
| `downloader` | youtubedl-android and FFmpeg media download logic. |
| `queue` | Download queue, concurrency, validation, and cleanup. |
| `notifications` | Android foreground notification. |

`BotEngine` is isolated from the Android UI. It receives incoming WhatsApp messages, parses commands, creates jobs, stores search sessions, and replies through the `WhatsAppClient` contract.

## 🛠️ Development

Requirements:

- Android Studio
- JDK 17
- Android SDK 35
- An Android device or emulator

Build and test:

```sh
./gradlew test assembleDebug
```

Install on a connected Android device:

```sh
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

If Wi-Fi ADB install hangs, this flow is often more reliable:

```sh
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/zapbot.apk
adb shell pm install -r -t /data/local/tmp/zapbot.apk
```

## 🔐 Permissions

The app requests permissions needed for a long-running bot:

- Internet/network access.
- Foreground service.
- Notifications.
- Boot completed.
- Wake lock.
- Wi-Fi high-performance lock.
- Battery optimization request shortcut.
- Contacts read access for friendlier log display when possible.

## ⚠️ Operational Notes

- Keep the bot phone charged.
- Disable battery optimization for the app.
- Keep WhatsApp linked-device session active.
- Large video uploads depend on WhatsApp and network conditions.
- The app does not read Android WhatsApp notifications; it connects through the linked-device bridge.

## 📚 Documentation

- Usage guide: [docs/USAGE.md](docs/USAGE.md)
- Architecture notes: [ARCHITECTURE.md](ARCHITECTURE.md)
- Testing notes: [TESTING.md](TESTING.md)

## 🧪 Current Status

ZapTube Bot is an actively evolving Android bot app. The core workflow is implemented:

- WhatsApp linked-device pairing.
- YouTube search.
- Video/audio download.
- WhatsApp media delivery.
- Job queue.
- Persistent settings.
- Logs and error filtering.
- Multilingual UI and bot responses.
