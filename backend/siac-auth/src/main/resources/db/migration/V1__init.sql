-- siac_auth: utilizadores, âmbitos (papel + condomínio/fração), refresh
-- tokens e registo de tentativas de login (para o lockout progressivo).
-- Corre com o role siac_auth_user, cujo search_path já aponta para este
-- schema (ver postgres/init/01-schemas-and-roles.sh).

CREATE TABLE utilizadores (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                 VARCHAR(255) NOT NULL UNIQUE,
    password_hash         VARCHAR(255) NOT NULL,
    nome                  VARCHAR(255) NOT NULL,
    ativo                 BOOLEAN NOT NULL DEFAULT true,
    deve_trocar_password  BOOLEAN NOT NULL DEFAULT false,
    criado_em             TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Um utilizador pode ter vários âmbitos (ex.: GESTOR_CONDOMINIO em vários
-- condomínios). SUPER_ADMIN_SIAC e ADMIN_SIAC não têm condominio_id/
-- fracao_id — o seu acesso é à plataforma toda, não a um âmbito.
CREATE TABLE ambitos (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id  UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    papel          VARCHAR(40) NOT NULL CHECK (papel IN (
                       'SUPER_ADMIN_SIAC', 'ADMIN_SIAC', 'GESTOR_CONDOMINIO',
                       'REPRESENTANTE_CONDOMINIO', 'TECNICO_FORNECEDOR', 'CONDOMINO'
                   )),
    condominio_id  UUID,
    fracao_id      UUID,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT ambitos_escopo_por_papel CHECK (
        (papel IN ('SUPER_ADMIN_SIAC', 'ADMIN_SIAC') AND condominio_id IS NULL AND fracao_id IS NULL)
        OR (papel NOT IN ('SUPER_ADMIN_SIAC', 'ADMIN_SIAC') AND condominio_id IS NOT NULL)
    )
);
CREATE INDEX idx_ambitos_utilizador ON ambitos(utilizador_id);

-- Refresh tokens são opacos ao cliente; só o hash (SHA-256) fica em BD.
-- Rotação a cada uso: um refresh usado é revogado e substituído por outro.
CREATE TABLE refresh_tokens (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    utilizador_id  UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
    token_hash     VARCHAR(128) NOT NULL UNIQUE,
    criado_em      TIMESTAMPTZ NOT NULL DEFAULT now(),
    expira_em      TIMESTAMPTZ NOT NULL,
    revogado_em    TIMESTAMPTZ
);
CREATE INDEX idx_refresh_tokens_utilizador ON refresh_tokens(utilizador_id);

-- RF-AUTH-06: log de autenticações falhadas, base do lockout progressivo.
CREATE TABLE tentativas_login (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email      VARCHAR(255) NOT NULL,
    sucesso    BOOLEAN NOT NULL,
    ip         VARCHAR(64),
    criado_em  TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_tentativas_login_email_criado ON tentativas_login(email, criado_em DESC);
