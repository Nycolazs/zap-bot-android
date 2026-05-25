# ZapTube Bot

Aplicativo Android Kotlin para rodar um bot de WhatsApp + YouTube em um telefone Android, pareado como aparelho conectado do WhatsApp.

## Estado da implementacao

- App Kotlin com Jetpack Compose e Material 3.
- Foreground Service com notificacao persistente e acao de parar.
- Room para settings, sessoes de pesquisa por chat, jobs e logs.
- Parser de comandos `/help`, `/{pesquisa}`, `/v1`, `/a1`, `/status`, `/cancel`.
- Motor do bot independente da UI.
- Fila de downloads com limite global e bloqueio de um download ativo por chat.
- Pesquisa real no YouTube via cliente web rapido, com cliente oficial Data API v3 tambem disponivel atras de `YouTubeSearchClient`.
- `FakeYouTubeSearchClient`, `FakeWhatsAppClient` e `FakeMediaDownloader` para teste local.
- `WhatsmeowWhatsAppClient` integrado via AAR gomobile/whatsmeow para parear como Linked Device no proprio aparelho Android.
- Downloads reais via youtubedl-android/FFmpeg, com envio de video ou audio pelo WhatsApp.

## Configuracao

1. Abra o projeto no Android Studio.
2. Defina a chave da YouTube Data API v3 em `~/.gradle/gradle.properties` ou `gradle.properties` local:

```properties
YOUTUBE_API_KEY=sua_chave
```

3. Execute `./gradlew test assembleDebug`.
4. Instale `app/build/outputs/apk/debug/app-debug.apk` em um aparelho Android.

## WhatsApp

O telefone que roda o bot pode ser diferente do telefone que possui o numero do WhatsApp, mas isso exige uma integracao real:

- WhatsApp Business Cloud API: caminho oficial, via webhooks HTTPS e numero Business/API.
- Linked Devices: caminho para conta pessoal, pareando o bot como aparelho conectado. Este projeto usa `whatsmeow` empacotado no APK via gomobile, sem depender de computador.

O app nao usa notificacoes do Android para ler mensagens, porque isso so funcionaria no telefone que recebe as notificacoes do proprio numero e nao atende a arquitetura de bot em aparelho separado.

## Uso

Veja [docs/USAGE.md](docs/USAGE.md) para instalar, parear o WhatsApp e usar os comandos.
