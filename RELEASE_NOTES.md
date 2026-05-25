# ZapTube Bot v1.1.0

## What's New

- Project and app name updated to ZapTube Bot.
- YouTube search results now include the video publication date.
- Video/audio delivery messages also include the publication date.
- Results still include title, channel, and duration.
- Downloads are selected with `/v1` and `/a1`, including support for replying to an older search message.
- Audio is sent as MP3 with title, channel/artist, and thumbnail cover art when available.
- The jobs screen only shows jobs from the last 24 hours.
- Runtime and error alerts are sent to the `Alerta Music Bot` WhatsApp group.

## How to Install

1. Download `ZapTube-Bot-v1.1.0.apk` from this release.
2. Install it on the Android phone that will run the bot.
3. Open the `ZapTube Bot` app.
4. In `Settings`, generate a pairing code using the WhatsApp phone number.
5. On the main WhatsApp phone, open `Linked devices` and connect using the pairing code.
6. Tap `Start bot`.

## Main Commands

- `/help`: show help.
- `/{search query}`: search YouTube. Example: `/pokemon opening`.
- `/v1`: send the video from result 1.
- `/a1`: send the audio from result 1.
- Reply to a search message with `/v1` or `/a1`: use the replied search results instead of the latest search in the chat.
