# ZapTube Bot v1.4.4

## What's New

- Added public Instagram and TikTok video URL downloads through `/v <url>`.
- Added automatic private-chat image-to-sticker conversion, disabled in groups.
- Improved sticker conversion so image proportions are preserved.
- Fixed repeated update downloads by reusing the already downloaded APK.
- Updated bot messages so user-facing bot replies follow the selected bot-message language.
- Added MIT license and README support links.

## Previous v1.4.3 Highlights

- Added a WhatsApp Blocklist in Settings with a modern add/remove number interface.
- Blocklisted numbers are ignored by the bot without disconnecting WhatsApp.
- Fixed bottom navigation page syncing.
- Improved dark mode system bar icon contrast.
- Improved bot-message language handling for all listed languages.

## Previous v1.4.2 Highlights

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
