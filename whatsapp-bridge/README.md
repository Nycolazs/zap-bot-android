# ZapTube Bot WhatsApp Bridge

Bridge local nao-oficial usando Baileys para conectar um WhatsApp pessoal por codigo de pareamento.

## Rodar

```bash
cd whatsapp-bridge
npm install
npm start
```

O app Android esta configurado para chamar `http://192.168.0.85:8787/`.

No app:

1. Abra `Ajustes`.
2. Digite o numero com DDI e DDD, por exemplo `5585999999999`.
3. Toque em `Gerar codigo de pareamento`.
4. No WhatsApp: `Aparelhos conectados` > `Conectar com numero de telefone`.
5. Digite o codigo mostrado no app.

## Observacao

Baileys usa o protocolo do WhatsApp Web e nao e uma API oficial da Meta. Pode quebrar com mudancas do WhatsApp e deve ser usado respeitando limites e politicas da plataforma.
