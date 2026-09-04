# SIASC — SIAS Condomínios

Sistema de gestão de condomínios. Backend Spring Boot em micro-serviços
containerizados, frontend React único, PostgreSQL, exposto via túnel
Cloudflare dedicado.

## Arranque rápido

```bash
cp .env.example .env   # preencher segredos
docker compose --env-file .env -f docker-compose.tunnel.yml up -d   # túnel
docker compose --env-file .env up -d                                # app (quando existir)
```

## Documentação

| Ficheiro | Conteúdo |
|---|---|
| `CLAUDE.md` | Guia do projeto e regras invioláveis |
| `docs/arquitetura.md` | Containers, redes, serviços, decisões |
| `docs/requisitos.md` | RF/RNF, papéis e âmbitos |
| `docs/tarefas.md` | Backlog por fases — estado atual do projeto |
| `docs/testes.md` | Estratégia de testes e Definition of Done |
| `TUNNEL.md` | Operação do túnel Cloudflare |
