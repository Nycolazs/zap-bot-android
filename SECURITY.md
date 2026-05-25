# Segurança e conformidade

- Nao coloque chaves de API no codigo-fonte.
- Nao baixe conteudo sem permissao, conteudo privado, pago, com DRM, bloqueio etario ou qualquer controle de acesso.
- A sessao do WhatsApp deve ficar em armazenamento privado do app.
- A acao "Limpar sessao do WhatsApp" deve apagar arquivos privados do adaptador real.
- Logs nao devem conter tokens, QR bruto persistido, cookies, sessoes ou caminhos sensiveis.
- Arquivos temporarios ficam em `cache/bot_jobs/{jobId}` e sao apagados apos envio quando configurado.
- Nomes de arquivo sao sanitizados para evitar path traversal.
- A notificacao de foreground nunca deve ser ocultada.
