# ZapTube Bot

Android Kotlin app for running a WhatsApp + YouTube bot on an Android phone, paired to WhatsApp as a linked device.

## Current Status

- Kotlin Android app built with Jetpack Compose and Material 3.
- Foreground Service with a persistent notification and a stop action.
- Room storage for settings, per-chat search sessions, jobs, and logs.
- Command parser for `/help`, `/{search query}`, `/v1`, `/a1`, `/status`, and `/cancel`.
- Bot engine isolated from the Android UI layer.
- Download queue with a global concurrency limit and one active download per chat.
- Real YouTube search through a fast web client, with an official YouTube Data API v3 client also available behind `YouTubeSearchClient`.
- `FakeYouTubeSearchClient`, `FakeWhatsAppClient`, and `FakeMediaDownloader` for local tests.
- `WhatsmeowWhatsAppClient` packaged through a gomobile/whatsmeow AAR to pair as a WhatsApp linked device directly on Android.
- Real media downloads through youtubedl-android/FFmpeg, with video or audio delivery through WhatsApp.

## Setup

1. Open the project in Android Studio.
2. Optionally set a YouTube Data API v3 key in `~/.gradle/gradle.properties` or a local `gradle.properties` file:

```properties
YOUTUBE_API_KEY=your_key
```

3. Run `./gradlew test assembleDebug`.
4. Install `app/build/outputs/apk/debug/app-debug.apk` on an Android device.

## WhatsApp

The phone running the bot can be different from the phone that owns the WhatsApp number, but this requires a real WhatsApp integration:

- WhatsApp Business Cloud API: official path, based on HTTPS webhooks and a Business/API number.
- Linked Devices: personal-account path, pairing the bot as a connected device. This project uses `whatsmeow` packaged into the APK through gomobile, without requiring a computer while the bot is running.

The app does not read Android notifications to process WhatsApp messages. Notification-based reading would only work on the phone that receives notifications for the same WhatsApp number, which does not fit this bot architecture.

## Usage

See [docs/USAGE.md](docs/USAGE.md) for installation, WhatsApp pairing, and command usage.
