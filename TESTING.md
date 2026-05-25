# Testes

Execute:

```bash
./gradlew test
```

Cobertura inicial:

- Parser de comandos.
- Sanitizacao de nomes de arquivos.
- Clientes fake de YouTube e downloader.

Para testar fluxo ponta a ponta sem WhatsApp real, injete `FakeWhatsAppClient`, `FakeYouTubeSearchClient` e `FakeMediaDownloader`, envie mensagens com `FakeWhatsAppClient.receive(...)` e valide `sentTexts`/`sentMedia`.

Casos que devem ser mantidos em testes ao evoluir:

- Sessoes de pesquisa separadas por `chatId`.
- Expiracao de sessao em 30 minutos.
- Um download ativo por chat.
- Downloads concorrentes entre chats diferentes.
- Estados `QUEUED`, `DOWNLOADING`, `SENDING`, `COMPLETED`, `FAILED`, `CANCELLED`.
- Limpeza de arquivos apos envio.
- Mapeamento de erros seguros para o usuario.
