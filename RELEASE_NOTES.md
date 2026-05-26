# ZapTube Bot v1.4.2

## What's New

- Blocked bot startup until WhatsApp has a saved linked-device session.
- Auto-start on boot now skips startup when WhatsApp is not paired or connected.
- Fixed update checking/downloading so network work runs off the Android main thread.
- Fixed bot-message language so search results, status labels, download captions, and user-facing errors follow the selected bot language.

## Previous v1.4.1 Highlights

- Added GitHub release update checking from the About card.
- When a newer APK is available, the app downloads it and opens the Android installer.
- If Android blocks APK installs from this app, it opens the required permission screen.
- The app is now locked to portrait orientation.

## Notes

Android does not allow a normal app to replace itself silently. The updater automates checking and downloading, then Android requires the user to confirm installation.

## Previous v1.4.0 Highlights

- App language and bot message language default to the Android system language.
- Unsupported system languages fall back to English.
- Redesigned Settings and improved light/dark theme colors.
