# Zappy WhatsApp Bridge

Local unofficial bridge using Baileys to connect a personal WhatsApp account with a pairing code.

## Run

```bash
cd whatsapp-bridge
npm install
npm start
```

The Android app is configured to call `http://192.168.0.85:8787/`.

In the app:

1. Open `Settings`.
2. Enter the phone number with country and area code, for example `5585999999999`.
3. Tap `Generate pairing code`.
4. In WhatsApp: `Linked devices` > `Link with phone number`.
5. Enter the code shown by the app.

## Note

Baileys uses the WhatsApp Web protocol and is not an official Meta API. It may break when WhatsApp changes its protocol and should be used while respecting platform limits and policies.
