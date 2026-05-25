# Using ZapTube Bot

## Install the APK

1. Download the APK from the GitHub release.
2. Move the file to the Android phone that will run the bot.
3. Allow installation from unknown sources when Android asks for permission.
4. Open ZapTube Bot.

## Pair WhatsApp

1. Open the `Settings` screen.
2. Enter the WhatsApp phone number with country and area code, using digits only. Example: `5585999999999`.
3. Tap `Generate pairing code`.
4. On the main WhatsApp phone, open `Linked devices`.
5. Choose `Link with phone number`.
6. Enter the code shown by ZapTube Bot.
7. When pairing is active, the integration area is locked and shows `OK`.

## Start the Bot

1. On the home screen, tap `Start bot`.
2. Keep the Android phone charged and, when possible, disable battery optimization for the app.
3. Create a WhatsApp group named `Alerta Music Bot` to receive activity, error, and low-battery alerts.

## WhatsApp Commands

- `/help`: show help.
- `/{search query}`: search YouTube. Example: `/zelda music`.
- `/v1`: download and send the video from result 1.
- `/a1`: download and send the audio from result 1.
- `/v`: when used as a reply to a search message, download the first video from that search.
- `/a`: when used as a reply to a search message, download the first audio from that search.
- `/status`: show the current bot status.
- `/cancel`: cancel the active job for the chat.

When replying to an older search message with `/v1` or `/a1`, ZapTube Bot uses the replied search results, not the latest search in the chat.

## Search and Delivery

Search replies show at least 8 results when available, including title, channel, duration, and publication date. Videos longer than 70 minutes are ignored.

Delivery messages also include title, channel, duration, and publication date. Audio is sent as MP3 with metadata: video title, channel name as artist, and thumbnail as cover art when available.
