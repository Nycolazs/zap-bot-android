# Como usar o ZapTube Bot

## Instalar o APK

1. Baixe o APK do release do GitHub.
2. Envie o arquivo para o celular Android que vai rodar o bot.
3. No Android, permita instalar apps de fontes desconhecidas quando o sistema pedir.
4. Abra o ZapTube Bot.

## Parear o WhatsApp

1. Abra a tela `Settings`.
2. Digite o numero do WhatsApp com DDI e DDD, somente numeros. Exemplo: `5585999999999`.
3. Toque em `Generate pairing code`.
4. No celular principal do WhatsApp, abra `Aparelhos conectados`.
5. Escolha `Conectar com numero de telefone`.
6. Digite o codigo exibido no ZapTube Bot.
7. Quando o pareamento ficar ativo, a area de integracao fica bloqueada com status `OK`.

## Ligar o bot

1. Na tela inicial, toque em `Start bot`.
2. Mantenha o Android com bateria suficiente e, se possivel, desative otimizacoes de bateria para o app.
3. Crie um grupo chamado `Alerta Music Bot` para receber avisos de atividade, erros e bateria baixa.

## Comandos no WhatsApp

- `/help`: mostra a ajuda.
- `/{pesquisa}`: pesquisa no YouTube. Exemplo: `/musica de zelda`.
- `/v1`: baixa e envia o video do resultado 1.
- `/a1`: baixa e envia o audio do resultado 1.
- `/v`: quando usado respondendo uma mensagem de pesquisa, baixa o primeiro video daquela pesquisa.
- `/a`: quando usado respondendo uma mensagem de pesquisa, baixa o primeiro audio daquela pesquisa.
- `/status`: mostra o estado atual do bot.
- `/cancel`: cancela o job ativo do chat.

Ao responder uma mensagem antiga de pesquisa com `/v1` ou `/a1`, o ZapTube Bot usa aquela pesquisa respondida, nao a pesquisa mais recente do chat.

## Pesquisa e entrega

A mensagem de pesquisa mostra pelo menos 8 resultados quando disponiveis, com titulo, canal, duracao e data de publicacao. Videos com mais de 70 minutos sao ignorados.

Na entrega, o bot tambem informa titulo, canal, duracao e data de publicacao. O audio e enviado em MP3 com metadados: titulo do video, artista como nome do canal e thumbnail como capa quando disponivel.
