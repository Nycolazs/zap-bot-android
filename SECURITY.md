# Security and Compliance

- Do not commit API keys to source control.
- Do not download content without permission, including private, paid, DRM-protected, age-restricted, or access-controlled content.
- WhatsApp session data must remain in private app storage.
- The `Clear WhatsApp session` action must remove private files used by the real adapter.
- Logs must not contain tokens, persisted raw QR data, cookies, sessions, or sensitive file paths.
- Temporary files are stored in `cache/bot_jobs/{jobId}` and are deleted after sending when configured.
- File names are sanitized to prevent path traversal.
- The foreground notification must never be hidden while the bot is running.
