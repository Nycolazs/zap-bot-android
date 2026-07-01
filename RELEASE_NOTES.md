# Zappy v1.4.7

## What's New

- Hardened the bot so it only handles private WhatsApp chats and ignores groups, status, broadcasts, and newsletters.
- Removed operational WhatsApp group alerts.
- Improved TikTok and Instagram video download handling with stricter MP4 output, upload-size limits, retries, and safer yt-dlp update attempts.
- Prevented oversized or unknown media from being sent as generic `.bin` files.
- Added sticker metadata: `Sticker created with Zappy BOT 🤖 by @nycolazs`.
- Added clearer runtime logs for bot start/stop, missing WhatsApp sessions, WhatsApp bridge state changes, and startup failures.
- Kept diagnostics in the Logs screen instead of adding extra status cards to Home.

## Previous v1.4.6 Highlights

- Added an Automation setting to enable or disable the one-time first-message welcome.
- The bot now respects the welcome setting before sending or recording first-contact welcomes.
- Added a Room migration so existing installs keep the welcome enabled by default.

## Previous v1.4.5 Highlights

- Renamed the project and app to Zappy.
- Added a one-time first-message welcome with basic commands in the selected bot-message language.
- Fixed relative published dates from YouTube search results so values such as `há 6 anos` are localized with the bot-message language.
- Updated the README with clearer setup, command, operation, and development guidance.
- Removed the old sticker slash commands from public usage; private-chat images are still converted to stickers automatically.

## Previous v1.4.4 Highlights

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
