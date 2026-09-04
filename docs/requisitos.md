# SIASC — Requisitos

Requisitos funcionais (RF) e não funcionais (RNF) da reconstrução do SIASC.
Organização por módulo = por micro-serviço. As fases referidas são as de
`docs/tarefas.md`.

## 1. Papéis e âmbitos

Papéis fixos do produto:

| Papel | Descrição |
|---|---|
| `ADMIN_SIAC` | Administração da plataforma (nós) |
| `GESTOR_CONDOMINIO` | Empresa/pessoa que administra um ou mais condomínios |
| `PORTEIRO_ZELADOR` | Operação diária no local |
| `TECNICO_FORNECEDOR` | Externo, acesso restrito às suas intervenções |
| `CONDOMINO` | Proprietário/residente de fração |

**Regra transversal (não negociável):** um papel, por si só, nunca concede
acesso a dados. Todo o utilizador (exceto `ADMIN_SIAC`) tem um ou mais
**âmbitos** explícitos — `condominio_id`, opcionalmente restringido a
`fracao_id` — e toda a query de todos os serviços filtra por esse âmbito.

## 2. Requisitos funcionais

### AUTH — `siac-auth` (fase 1)

- **RF-AUTH-01** — Login com email + password (hash Argon2/BCrypt); resposta
  com JWT de curta duração + refresh token.
- **RF-AUTH-02** — O JWT transporta papéis e âmbitos do utilizador.
- **RF-AUTH-03** — Endpoint JWKS interno para validação do token pelos
  restantes serviços.
- **RF-AUTH-04** — Gestão de utilizadores (criar, desativar, repor password)
  por `ADMIN_SIAC`; gestores podem criar utilizadores **dentro dos seus
  âmbitos**.
- **RF-AUTH-05** — Atribuição e revogação de âmbitos a utilizadores.
- **RF-AUTH-06** — Registo (log) de autenticações falhadas e bloqueio
  progressivo por tentativas.

### CORE — `siac-core` (fase 2)

- **RF-CORE-01** — CRUD de condomínios (dados administrativos, morada).
- **RF-CORE-02** — CRUD de blocos e frações (permilagem, tipologia, estado).
- **RF-CORE-03** — CRUD de pessoas e ligação pessoa↔fração
  (proprietário/inquilino, datas).
- **RF-CORE-04** — Dashboard resumo por condomínio (contagens, alertas),
  sempre filtrado pelo âmbito do token.
- **RF-CORE-05** — `CONDOMINO` vê apenas os dados do seu condomínio e da(s)
  sua(s) fração(ões).

### OPER — `siac-operacoes` (fase 3)

- **RF-OPER-01** — Registo de ocorrências (título, descrição, fração/zona,
  prioridade, fotos), por qualquer papel com âmbito no condomínio.
- **RF-OPER-02** — Fluxo de estados da ocorrência: aberta → em curso →
  resolvida/fechada, com histórico.
- **RF-OPER-03** — CRUD de equipamentos e plano de manutenções.
- **RF-OPER-04** — CRUD de fornecedores; `TECNICO_FORNECEDOR` só vê
  ocorrências/manutenções que lhe estão atribuídas.

### FIN — `siac-financeiro` (fase 4)

- **RF-FIN-01** — Definição de quotas por fração (permilagem ou valor fixo).
- **RF-FIN-02** — Lançamento de movimentos (receitas/despesas) por condomínio.
- **RF-FIN-03** — Mapa de quotas: pagas, pendentes, em atraso.
- **RF-FIN-04** — `CONDOMINO` vê apenas o extrato da sua fração.

### ASSEMB — `siac-assembleias` (fase 5)

- **RF-ASSEMB-01** — Criação de convocatórias com ordem de trabalhos.
- **RF-ASSEMB-02** — Registo de presenças e representações.
- **RF-ASSEMB-03** — Atas e deliberações consultáveis pelos condóminos do
  condomínio.

### FRONT — `siac-frontend` (transversal)

- **RF-FRONT-01** — SPA única com login; sem sessão não há acesso a nenhuma
  rota além de `/login`.
- **RF-FRONT-02** — Navegação e widgets adaptados ao papel/âmbito do
  utilizador autenticado.
- **RF-FRONT-03** — Seleção de condomínio ativo quando o utilizador tem
  vários âmbitos.

## 3. Requisitos não funcionais

- **RNF-SEG-01 (gate de segurança)** — Nenhum endpoint de dados é exposto,
  nem o hostname público ativado, antes de RF-AUTH-01..03 estarem
  implementados e testados. Dados reais (PII/financeiros) só entram no
  sistema depois da filtragem por âmbito estar testada (ver `docs/testes.md`).
- **RNF-SEG-02** — Toda a resposta a pedido sem token válido é 401; com token
  válido mas âmbito errado é 403. Sem exceções.
- **RNF-SEG-03** — Segredos apenas em `.env`/secrets; TLS terminado na
  Cloudflare; portas dos serviços nunca publicadas no host.
- **RNF-ISOL-01** — Zero comunicação com as stacks home03/lostandfound/
  Nextcloud: sem redes, volumes ou containers partilhados.
- **RNF-PERF-01** — A stack completa da fase ativa tem de caber nos recursos
  do Pi 5 com as outras stacks a correr; todos os containers com `mem_limit`
  e `cpus` (ver tabela em `docs/arquitetura.md`).
- **RNF-OPS-01** — Backup diário do Postgres; arranque automático
  (`restart: unless-stopped`); logs consultáveis por `docker logs`.
- **RNF-DADOS-01** — Migrações de schema exclusivamente via Flyway,
  versionadas no repositório do serviço dono do schema.
