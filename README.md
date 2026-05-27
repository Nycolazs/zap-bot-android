# Zappy

[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](#requirements)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)](#development)
[![License](https://img.shields.io/badge/License-MIT-blue)](LICENSE)
[![Release](https://img.shields.io/badge/Release-v1.4.5-111827)](https://github.com/Nycolazs/zap-bot-android/releases)

Zappy is a native Android app that turns an Android phone into a WhatsApp-linked media bot. It pairs with WhatsApp as a linked device, listens for slash commands, searches YouTube, downloads media on-device, and sends audio, video, playlists, Instagram videos, TikTok videos, and private-chat stickers back to WhatsApp.

Built with Kotlin, Jetpack Compose, Material 3, Room, youtubedl-android, FFmpeg, and a gomobile/whatsmeow bridge.

## Contents

- [What Zappy Does](#what-zappy-does)
- [Download](#download)
- [Quick Start](#quick-start)
- [WhatsApp Commands](#whatsapp-commands)
- [Languages](#languages)
- [Settings](#settings)
- [Reliability Notes](#reliability-notes)
- [Development](#development)
- [Architecture](#architecture)
- [Troubleshooting](#troubleshooting)

## What Zappy Does

| Area | Capability |
| --- | --- |
| WhatsApp | Connects as a linked device and replies directly in chats. |
| YouTube search | Searches YouTube from WhatsApp with a simple `/search text` or `/text` command. |
| Video delivery | Downloads and sends selected YouTube, Instagram, and TikTok videos. |
| Audio delivery | Downloads YouTube audio and sends it as an audio file. |
| Playlists | Downloads YouTube playlists as MP3 files and sends them in a `.zip`. |
| Stickers | Converts images sent in private chats into WhatsApp stickers automatically. |
| Queue | Runs multiple downloads with configurable concurrency and per-chat status. |
| App dashboard | Shows bot state, uptime, active jobs, logs, errors, and settings. |
| Updates | Checks GitHub Releases and opens the APK installer when a newer version exists. |

## Download

Get the latest APK from GitHub Releases:

https://github.com/Nycolazs/zap-bot-android/releases

Current version:

- `versionName`: `1.4.5`
- `versionCode`: `10`
- `minSdk`: `26`
- `targetSdk`: `35`

## Quick Start

1. Download the APK from the latest GitHub release.
2. Install it on the Android phone that will run Zappy.
3. Open **Zappy**.
4. Go to **Settings**.
5. Enter your WhatsApp number with country and area code.
6. Generate the Linked Devices code.
7. On your main WhatsApp phone, open `WhatsApp > Linked devices > Link with phone number`.
8. Enter the code shown in Zappy.
9. Return to **Home** and tap **Start bot**.

For best reliability, keep the phone charged and disable Android battery optimization for Zappy.

## WhatsApp Commands

| Command | What it does |
| --- | --- |
| `/help` | Shows the full bot help message. |
| `/{search query}` | Searches YouTube. Example: `/zelda music`. |
| `/search zelda music` | Searches YouTube explicitly. |
| `/pesquisar zelda music` | Searches YouTube explicitly in Portuguese. |
| `/v1` | Downloads and sends video result 1. |
| `/a1` | Downloads and sends audio result 1. |
| `/v` | Downloads video result 1, useful as a reply to a search result message. |
| `/a` | Downloads audio result 1, useful as a reply to a search result message. |
| `/v https://youtu.be/...` | Downloads and sends a YouTube video link. |
| `/a https://youtu.be/...` | Downloads and sends audio from a YouTube link. |
| `/a https://www.youtube.com/playlist?...` | Downloads playlist audio and sends a `.zip`. |
| `/v https://www.instagram.com/reel/...` | Downloads and sends an Instagram video. |
| `/v https://www.tiktok.com/@user/video/...` | Downloads and sends a TikTok video. |
| `/status` | Shows the active download status for that chat. |
| `/cancel` | Cancels the active job for that chat. |

Replying to an older Zappy search result with `/v1` or `/a1` uses that replied result list, even if the chat has a newer search.

Direct-link rules:

- YouTube links can use `/v` for video or `/a` for audio.
- YouTube playlists are supported with `/a` only.
- Instagram and TikTok links are supported with `/v` only.
- Images sent in private chats become stickers automatically; groups are ignored for sticker conversion.

When a person sends their first message to Zappy, the bot sends a one-time welcome message in the selected bot-message language with basic commands and `/help`.

## Languages

Zappy has two independent language settings:

| Setting | Controls |
| --- | --- |
| App language | Android UI labels and screens. |
| Bot message language | WhatsApp replies sent by the bot. |

Available languages:

- English
- Portuguese
- Spanish
- Russian

Search results, status labels, download captions, welcome text, and published-date labels follow the selected bot-message language.

## Settings

The Settings screen includes:

- WhatsApp Linked Devices pairing.
- App language and bot-message language.
- Light, dark, and system theme modes.
- Start on boot.
- Foreground notifications and detailed notifications.
- Battery settings shortcut.
- Concurrent download count.
- Video quality limit.
- Audio bitrate preference.
- Network preference.
- Blocklisted numbers.
- WhatsApp cache and session clearing.

Settings are persisted with Room and survive app restarts.

## Reliability Notes

Zappy runs as a foreground service because Android requires a visible, user-controlled notification for long-running background work. While running, it uses wake and Wi-Fi locks to improve download reliability.

Recommended setup:

- Keep the bot phone charged.
- Disable battery optimization for Zappy.
- Keep the WhatsApp linked-device session active.
- Prefer Wi-Fi for large video uploads.
- Keep enough free storage for downloads and temporary files.

Operational limits:

- Large uploads still depend on WhatsApp, network quality, and device performance.
- The linked-device integration is unofficial and can break if WhatsApp changes its protocol.
- Zappy does not read Android WhatsApp notifications; it connects through the linked-device bridge.

## Development

### Requirements

- Android Studio
- JDK 17
- Android SDK 35
- Android device or emulator

### Build and Test

```sh
./gradlew test assembleDebug
```

### Install on a Connected Device

```sh
adb install -r -t app/build/outputs/apk/debug/app-debug.apk
```

If Wi-Fi ADB install hangs, use push plus package manager install:

```sh
adb push app/build/outputs/apk/debug/app-debug.apk /data/local/tmp/zappy.apk
adb shell pm install -r -t /data/local/tmp/zappy.apk
```

## Architecture

| Layer | Responsibility |
| --- | --- |
| `ui` | Compose screens, theme, localized UI strings, and ViewModel. |
| `service` | Foreground service, boot receiver, runtime state, wake and Wi-Fi locks. |
| `domain` | Bot commands, models, parser, engine, and localized bot messages. |
| `database` | Room entities, DAOs, converters, and migrations. |
| `settings` | Settings repository and normalization. |
| `whatsapp` | WhatsApp client contract and whatsmeow bridge implementation. |
| `youtube` | YouTube search clients. |
| `downloader` | youtubedl-android and FFmpeg media download logic. |
| `queue` | Download queue, concurrency, validation, and cleanup. |
| `notifications` | Android foreground notification. |

`BotEngine` is isolated from the Android UI. It receives incoming WhatsApp messages, parses commands, creates jobs, stores search sessions, sends first-message welcomes, and replies through the `WhatsAppClient` contract.

## Documentation

- Usage guide: [docs/USAGE.md](docs/USAGE.md)
- Architecture notes: [ARCHITECTURE.md](ARCHITECTURE.md)
- Testing notes: [TESTING.md](TESTING.md)
- Security notes: [SECURITY.md](SECURITY.md)

## Troubleshooting

When opening an issue, include:

- App version.
- Android version and device model.
- WhatsApp command or app action that triggered the issue.
- Relevant Zappy logs or screenshots, with private chat data removed.

Issues: https://github.com/Nycolazs/zap-bot-android/issues

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE).
