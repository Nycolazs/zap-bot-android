# Zap Bot Android

Projeto Android Kotlin para uma base profissional de bot WhatsApp + YouTube executando em um telefone com Foreground Service.

## Estado da implementacao

- App Kotlin com Jetpack Compose e Material 3.
- Foreground Service com notificacao persistente e acao de parar.
- Room para settings, sessoes de pesquisa por chat, jobs e logs.
- Parser de comandos `/help`, `/pesquisa`, `/v`, `/a`, `/status`, `/cancel`.
- Motor do bot independente da UI.
- Fila de downloads com limite global e bloqueio de um download ativo por chat.
- Cliente YouTube oficial via Data API v3 atras de `YouTubeSearchClient`.
- `FakeYouTubeSearchClient`, `FakeWhatsAppClient` e `FakeMediaDownloader` para teste local.
- `WhatsmeowWhatsAppClient` integrado via AAR gomobile/whatsmeow para parear como Linked Device no proprio aparelho Android.
- `QrBridgeWhatsAppClient`, `BridgeWhatsAppClient` e `YtDlpMediaDownloader` como adaptadores substituiveis, sem fingir integracao real.

## Configuracao

1. Abra o projeto no Android Studio.
2. Defina a chave da YouTube Data API v3 em `~/.gradle/gradle.properties` ou `gradle.properties` local:

```properties
YOUTUBE_API_KEY=sua_chave
```

3. Configure uma integracao real do WhatsApp atras de `WhatsAppClient`.
4. Execute `./gradlew test` e depois rode o app em um aparelho Android.

## WhatsApp

O telefone que roda o bot pode ser diferente do telefone que possui o numero do WhatsApp, mas isso exige uma integracao real:

- WhatsApp Business Cloud API: caminho oficial, via webhooks HTTPS e numero Business/API.
- Linked Devices: caminho para conta pessoal, pareando o bot como aparelho conectado. Este projeto usa `whatsmeow` empacotado no APK via gomobile, sem depender de computador.

O app nao usa notificacoes do Android para ler mensagens, porque isso so funcionaria no telefone que recebe as notificacoes do proprio numero e nao atende a arquitetura de bot em aparelho separado.

## Downloader

Downloads reais exigem um componente local compativel com Android, como uma distribuicao propria de yt-dlp/FFmpeg ou servico autorizado. O esqueleto atual recusa downloads reais ate essa integracao ser feita. Baixe apenas conteudos que voce tem direito de usar.
