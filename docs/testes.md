# SIASC — Testes

Estratégia de testes da stack. Regra geral: os testes correm no build de cada
serviço (`./mvnw verify` / `npm test`) e têm de passar antes de qualquer
imagem ser reconstruída e posta a correr no Pi.

## 1. Pirâmide por serviço Spring Boot

1. **Unitários** — JUnit 5 + Mockito para regras de negócio puras (cálculo de
   quotas, transições de estado de ocorrências, montagem de âmbitos).
2. **Slice tests** — `@WebMvcTest` para controllers (validação, códigos HTTP,
   serialização) e `@DataJpaTest` para repositórios com queries próprias.
3. **Integração** — Testcontainers com `postgres:17` (funciona em ARM64):
   sobe o serviço completo contra Postgres real, aplica migrações Flyway e
   exercita os endpoints via `WebTestClient`/`MockMvc`. As migrações são
   testadas aqui — nunca `ddl-auto` em testes de integração.

## 2. Testes de segurança (obrigatórios, o gate do projeto)

Nenhum endpoint de dados entra em `main` sem esta matriz coberta no serviço
respetivo:

| Caso | Esperado |
|---|---|
| Sem token | 401 |
| Token expirado / assinatura inválida | 401 |
| Token válido, sem âmbito no condomínio pedido | 403 |
| Token válido, âmbito noutro condomínio (troca de `condominio_id` no pedido) | 403 |
| `CONDOMINO` a pedir dados de fração que não é sua | 403 |
| Token válido com âmbito correto | 200 e **apenas** dados do âmbito |

Implementação: classe de teste base partilhada por serviço que gera JWTs de
teste (chave RS256 de teste) para cada papel × âmbito e corre a matriz sobre
cada endpoint novo. No `siac-auth`, juntar ainda: lockout após N falhas,
refresh token revogado → 401, e hash de password nunca presente em respostas.

## 3. Frontend

- **Unitários/componentes** — Vitest + React Testing Library: guarda de
  rotas (sem sessão → `/login`), renderização condicionada ao papel,
  seleção de condomínio ativo.
- **Sem contrato implícito** — chamadas à API centralizadas num client
  tipado; alterações de contrato quebram compilação TS, não a runtime.

## 4. Smoke tests da stack (no Pi)

Script `scripts/smoke.sh` (a criar na fase 1) corrido após cada `up -d`:

```bash
docker ps --filter name=siac- --format '{{.Names}} {{.Status}}'   # tudo healthy
# na rede interna:
curl -fs http://siac-auth:8080/actuator/health
curl -fs -o /dev/null -w '%{http_code}' http://siac-core:8080/api/core/condominios   # 401 sem token
```

O teste de 401 sem token faz parte do smoke — se um dia devolver 200, a
stack está mal e o deploy pára aí.

## 5. Isolamento e operação

- Verificação periódica de que nenhum container `siac-*` está ligado a redes
  de outras stacks (`docker inspect` nas redes) — parte do smoke.
- Restauro de backup testado de verdade (fase 6): `pg_restore` para uma base
  descartável + contagem de linhas por tabela.

## 6. Definition of Done (resumo)

Uma tarefa de `docs/tarefas.md` só fecha quando:

1. `./mvnw verify` (ou `npm test`) passa localmente no serviço tocado;
2. endpoints novos têm a matriz 401/403 do §2;
3. migrações Flyway aplicam de raiz num Postgres limpo (coberto pelos testes
   Testcontainers);
4. o smoke da stack passa no Pi;
5. `docs/tarefas.md` atualizado.
