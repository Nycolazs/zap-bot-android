# Testing

Run:

```bash
./gradlew test
```

Initial coverage:

- Command parser.
- File name sanitization.
- Fake YouTube and downloader clients.

To test the end-to-end flow without a real WhatsApp connection, inject `FakeWhatsAppClient`, `FakeYouTubeSearchClient`, and `FakeMediaDownloader`, send messages with `FakeWhatsAppClient.receive(...)`, and validate `sentTexts`/`sentMedia`.

Cases that should remain covered as the project evolves:

- Search sessions isolated by `chatId`.
- Session expiration after 30 minutes.
- One active download per chat.
- Concurrent downloads across different chats.
- Job states: `QUEUED`, `DOWNLOADING`, `SENDING`, `COMPLETED`, `FAILED`, `CANCELLED`.
- File cleanup after sending.
- Safe error mapping for user-facing messages.
