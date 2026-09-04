# SIASC — Tarefas

Backlog por fases. **Este ficheiro é a fonte de verdade do estado do
projeto** — atualizar as checkboxes sempre que algo avança. Uma fase só abre
quando a anterior fecha (exceção: documentação e frontend podem andar em
paralelo).

## Fase 0 — Fundação

- [x] Reaproveitar túnel Cloudflare `siac` (`docker-compose.tunnel.yml`, `TUNNEL.md`)
- [x] Estrutura do repositório + documentação base (CLAUDE.md, docs/)
- [x] `git init` + primeiro commit + repo remoto (`thiagorabaioli/siasc-v2`)
- [x] `.env.example` completo e `.gitignore`
- [x] `docker-compose.yml` com `siac-postgres` (rede `siac-internal`, volume, limites, healthcheck)
- [x] `postgres/init/`: base `siac`, schemas e um role por serviço com grants restritos
- [x] Esqueleto `siac-frontend` (Vite + React + TS, Dockerfile nginx non-root, proxy `/api/`)
- [x] Validar: túnel up, postgres up, frontend acessível na rede interna (sem hostname público)
- [x] Script de backup diário do Postgres (`pg_dump` → `~/backups_local`)

## Fase 1 — Segurança (gate: nada de dados antes disto)

- [ ] Esqueleto `backend/siac-auth` (Maven, Spring Boot 3, Flyway, Dockerfile multi-stage arm64)
- [ ] Modelo `siac_auth`: utilizadores, papéis, âmbitos, refresh tokens
- [ ] RF-AUTH-01 login + JWT RS256 + refresh
- [ ] RF-AUTH-02 papéis e âmbitos no token
- [ ] RF-AUTH-03 endpoint JWKS interno
- [ ] RF-AUTH-06 lockout progressivo + log de falhas
- [ ] Frontend: página de login, guarda de rotas, token em memória + refresh
- [ ] Testes de segurança da matriz 401/403 (ver `docs/testes.md`)
- [ ] Seed do utilizador `ADMIN_SIAC` inicial (password via `.env`, forçar troca)
- [ ] **Só aqui**: ativar hostname `siasc.sias.pt` → `siac-frontend` no túnel

## Fase 2 — Núcleo (`siac-core`)

`siac-core` absorve o antigo `siac-operacoes` (decisão de 2026-09-04, ver
`docs/arquitetura.md` §8) — um único serviço/schema cobre condomínios,
frações, pessoas e operações do dia a dia.

- [ ] Esqueleto `backend/siac-core` como resource server (valida JWT via JWKS)
- [ ] Filtro de âmbito transversal (toda a query filtrada; mismatch → 403) + testes
- [ ] RF-CORE-01 condomínios
- [ ] RF-CORE-02 blocos e frações
- [ ] RF-CORE-03 pessoas e ligação pessoa↔fração
- [ ] RF-CORE-04 dashboard por condomínio
- [ ] RF-CORE-06/07 ocorrências com fluxo de estados
- [ ] RF-CORE-08 equipamentos e manutenções
- [ ] RF-CORE-09 fornecedores + visão restrita do `TECNICO_FORNECEDOR`
- [ ] RF-AUTH-04/05 gestão de utilizadores e âmbitos (UI + API)
- [ ] Frontend: páginas condomínios, frações, pessoas, dashboard, ocorrências, equipamentos, fornecedores
- [ ] Seed demo idempotente (condomínios fictícios) para desenvolvimento

## Fase 3 — Financeiro (`siac-financeiro`, rename planeado para `siac-billing`)

- [ ] Esqueleto do serviço + schema + filtro de âmbito
- [ ] RF-FIN-01 quotas por fração
- [ ] RF-FIN-02 movimentos
- [ ] RF-FIN-03 mapa de quotas
- [ ] RF-FIN-04 extrato do condómino
- [ ] Frontend: financeiro

## Fase 4 — Assembleias (`siac-assembleias`)

- [ ] Esqueleto do serviço + schema + filtro de âmbito
- [ ] RF-ASSEMB-01/02/03 convocatórias, presenças, atas
- [ ] Frontend: assembleias

## Fase 5 — Operação e comercialização (v2)

- [ ] Monitorização básica (healthchecks agregados, alertas)
- [ ] Restauro de backup testado (não só o dump)
- [ ] Onboarding de condomínio real (só após auditoria da matriz de âmbitos)
- [ ] Planos/billing e restantes itens de `siascvendasv2` (doc a recuperar)

## Registo de decisões / notas

- 2026-09-04 — Reinício do projeto: backend passa de Node/Express (v1) para
  Spring Boot em micro-serviços; túnel e redes da v1 mantêm-se. Docs da v1
  (`siascgeral.md`, `siascv1.arquitetura.md`, `modelo-dados.md`,
  `siascvendasv2.md`) já não estão no diretório — recuperar do repo antigo o
  que for útil (modelo de dados e doc de vendas em particular).
- 2026-09-04 — `siac-operacoes` fundido em `siac-core` (um serviço a menos a
  correr no Pi); `siac-financeiro` tem rename planeado para `siac-billing`
  (execução adiada para a Fase 3). Ver `docs/arquitetura.md` §8.
- 2026-09-04 — Sessão de implementação: como Fase 0 e Fase 1 ainda não
  estavam fechadas, decidiu-se fechá-las primeiro em vez de avançar
  diretamente para `siac-core`, respeitando a ordem de fases deste
  documento.
