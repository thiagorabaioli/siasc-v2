# Túnel Cloudflare do SIAC

O SIAC usa um túnel Cloudflare dedicado chamado `siac`, com o ID
`230328df-d1bd-4f39-a632-7fe11ff37ec2`. O contentor local chama-se
`siac-tunnel`. Não reutiliza o token, o contentor nem a rede do
`lostandfound-tunnel`.

## Criação no painel

1. Abrir Cloudflare Zero Trust.
2. Aceder a **Networks > Tunnels**.
3. Criar um túnel do tipo **Cloudflared** com o nome `siac-tunnel`.
4. Escolher Docker como ambiente do conector.
5. Copiar somente o valor apresentado depois de `--token`.
6. Criar `.env` nesta pasta, a partir de `.env.example`, e guardar o valor em
   `CLOUDFLARE_TUNNEL_TOKEN`.

Não colar o token em documentação, commits, mensagens ou comandos guardados no
histórico.

## Arranque

```bash
docker compose --env-file .env -f docker-compose.tunnel.yml up -d
```

## Validação

```bash
docker ps --filter name=^/siac-tunnel$
docker logs --tail 100 siac-tunnel
```

Os hostnames públicos só devem ser adicionados depois de existirem os serviços
SIAC na rede `siac-edge`.
