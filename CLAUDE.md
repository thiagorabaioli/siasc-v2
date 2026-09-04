# SIASC — SIAS Condomínios

Sistema de gestão de condomínios (produto autónomo da família SIAS). Corre num
Raspberry Pi 5 partilhado com outras stacks (home03/lostandfound, Nextcloud),
mas é **totalmente independente delas**: túnel próprio (`siac`), redes próprias
(`siac-edge`, `siac-internal`), Postgres próprio, repositório próprio.
Endereço público: `https://siasc.sias.pt/` (só ativar hostname depois da
autenticação estar implementada).

## Stack

- **Backend**: Java 21 + Spring Boot 3.x — um micro-serviço por domínio, cada
  um no seu container (`siac-auth`, `siac-core que inclui operaçoes docker container:

condominium-core
        │
        ├── Condomínios
        ├── Frações
        ├── Pessoas
        ├── Ocorrências
        ├── Equipamentos
        ├── Manutenções
        ├── Documentos
        └── Fornecedores`, 

BILLING SERVICE
│
├── QUOTA
│
├── PAGAMENTO
│
├── RECIBO
│
├── FATURA
│
├── DESPESA
│
├── ORCAMENTO
│
└── CONTA_CORRENTE

        
BILLING DB

QUOTA
--------------------
id
tenant_id
condominio_id
fracao_id
valor
vencimento
estado


. Migrações com Flyway.
- **Frontend**: único — React + TypeScript + Vite, servido por nginx
  (`siac-frontend`), que faz reverse-proxy de `/api/<serviço>/` para cada
  backend. É o único ponto de entrada HTTP.
- **Base de dados**: PostgreSQL 17 (`siac-postgres`), uma instância, **um
  schema por serviço** (`siac_auth`, `siac_core`, `siac_operacoes`,
  `siac_financeiro`, `siac_assembleias`). Cada serviço tem o seu utilizador de
  BD com acesso apenas ao seu schema.
- **Exposição**: túnel Cloudflare `siac` já existente
  (`docker-compose.tunnel.yml`, ver `TUNNEL.md`).

## Estrutura do repositório

```
siac/
├── CLAUDE.md                  # este ficheiro
├── TUNNEL.md                  # operação do túnel Cloudflare (reaproveitado)
├── docker-compose.tunnel.yml  # túnel (reaproveitado, não mexer sem razão)
├── docker-compose.yml         # serviços da aplicação (a criar)
├── .env / .env.example        # segredos; .env NUNCA vai para o git
├── docs/
│   ├── arquitetura.md         # containers, redes, fluxos, modelo de dados
│   ├── requisitos.md          # RF/RNF por módulo, papéis e âmbitos
│   ├── tarefas.md             # backlog por fases (fonte de verdade do estado)
│   └── testes.md              # estratégia e regras de testes
├── backend/
│   ├── siac-auth/             # autenticação, utilizadores, papéis, âmbitos
│   ├── siac-core/             # condomínios, frações, pessoas
│   └── ...                    # restantes serviços, criados por fase
├── frontend/                  # app React única + nginx.conf
└── postgres/init/             # scripts de bootstrap (schemas, utilizadores)
```

## Regras invioláveis

1. **Segurança antes de dados reais.** Ordem obrigatória: autenticação
   (`siac-auth` + login no frontend) → autorização por âmbito em todas as
   queries → só depois endpoints com dados. Nenhum endpoint de dados pode ir
   para além de dados demo enquanto não validar JWT **e** âmbito. Não repetir o
   erro da v1 (API pública sem auth).
2. **Papel nunca chega.** Papéis fixos: `SUPER_ADMIN_SIAC`,`ADMIN_SIAC`, `GESTOR_CONDOMINIO`,
   `REPRESENTANTE_CONDOMINIO`, `TECNICO_FORNECEDOR`, `CONDOMINO`. Todo o acesso exige
   também um **âmbito** explícito (`condominio_id`, opcionalmente
   `fracao_id`). Um `condominio_id` vindo do cliente nunca é confiável — tem
   de ser validado contra os âmbitos do token.
3. **Isolamento de rede.** Nunca ligar containers SIASC a redes de outras
   stacks (`lostandfound-network`, etc.) nem partilhar volumes com elas.
   `siac-internal` é `internal: true` (sem saída para fora) — Postgres e
   backends vivem só aí; apenas `siac-frontend` e `siac-tunnel` tocam em
   `siac-edge`.
4. **Segredos.** Token do túnel, passwords de BD e chaves JWT só em `.env` /
   secrets — nunca em commits, docs, ou comandos guardados no histórico.
5. **Recursos do Pi.** Todos os containers têm `mem_limit` e `cpus` definidos.
   Serviços Spring Boot arrancam por fase — não subir os 5 de uma vez sem
   necessidade.
6. **Containers non-root** sempre que possível (imagens `eclipse-temurin` com
   user próprio; nginx com `user: "101:101"`).

## Comandos

```bash
# Túnel (já operacional)
docker compose --env-file .env -f docker-compose.tunnel.yml up -d

# Aplicação (quando docker-compose.yml existir)
docker compose --env-file .env up -d

# Build de um serviço
cd backend/siac-auth && ./mvnw -q verify

# Estado
docker ps --filter name=siac-
```

## Documentação

- `docs/arquitetura.md` — desenho técnico; consultar antes de criar serviços.
- `docs/requisitos.md` — o que cada módulo faz e para quem.
- `docs/tarefas.md` — **atualizar sempre** que uma tarefa avança ou termina.
- `docs/testes.md` — nenhum endpoint novo sem os testes ali exigidos.
