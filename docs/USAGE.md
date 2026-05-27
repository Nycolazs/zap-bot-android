# Using Zappy

## Install the APK

1. Download the APK from the GitHub release.
2. Move the file to the Android phone that will run the bot.
3. Allow installation from unknown sources when Android asks for permission.
4. Open Zappy.

## Pair WhatsApp

1. Open the `Settings` screen.
2. Enter the WhatsApp phone number with country and area code, using digits only. Example: `5585999999999`.
3. Tap `Generate pairing code`.
4. On the main WhatsApp phone, open `Linked devices`.
5. Choose `Link with phone number`.
6. Enter the code shown by Zappy.
7. When pairing is active, the integration area is locked and shows `OK`.

## Start the Bot

1. On the home screen, tap `Start bot`.
2. Keep the Android phone charged and, when possible, disable battery optimization for the app.
3. Create a WhatsApp group named `Alerta Zappy` to receive activity, error, and low-battery alerts.

## WhatsApp Commands

- `/help`: show help.
- `/{search query}`: search YouTube. Example: `/zelda music`.
- `/search zelda music`: search YouTube explicitly.
- `/v1`: download and send the video from result 1.
- `/a1`: download and send the audio from result 1.
- `/v`: when used as a reply to a search message, download the first video from that search.
- `/a`: when used as a reply to a search message, download the first audio from that search.
- `/v https://youtu.be/...`: download and send a YouTube video link.
- `/a https://youtu.be/...`: download and send audio from a YouTube link.
- `/a https://www.youtube.com/playlist?...`: download playlist audio and send a `.zip`.
- `/v https://www.instagram.com/reel/...`: download and send an Instagram video.
- `/v https://www.tiktok.com/@user/video/...`: download and send a TikTok video.
- `/status`: show the current bot status.
- `/cancel`: cancel the active job for the chat.

When replying to an older search message with `/v1` or `/a1`, Zappy uses the replied search results, not the latest search in the chat.

Images sent in private chats are converted into stickers automatically. Sticker conversion is ignored in groups.

## Search and Delivery

Search replies show at least 8 results when available, including title, channel, duration, and publication date. Videos longer than 70 minutes are ignored.

Delivery messages also include title, channel, duration, and publication date. Audio is sent as MP3 with metadata: video title, channel name as artist, and thumbnail as cover art when available.
