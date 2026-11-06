-- Schema inicial completo do Fintech Agent (versão consolidada, projeto ainda sem produção).

-- Usuários e contas --------------------------------------------------------------------------------
CREATE TABLE app_user (
    id            UUID PRIMARY KEY,
    email         VARCHAR(160) NOT NULL UNIQUE,
    name          VARCHAR(120) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Número público da conta é o que outras pessoas usam para transferir.
CREATE TABLE account (
    id             UUID PRIMARY KEY,
    owner_id       UUID        NOT NULL UNIQUE REFERENCES app_user (id),
    account_number VARCHAR(10) NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_account_number UNIQUE (account_number)
);

-- Transferências entre contas do sistema ----------------------------------------------------------
CREATE TABLE transfers (
    id                     UUID PRIMARY KEY,
    origin_account_id      UUID         NOT NULL REFERENCES account (id),
    destination_account_id UUID         NOT NULL REFERENCES account (id),
    amount_cents           BIGINT       NOT NULL CHECK (amount_cents > 0),
    description            VARCHAR(120),
    idempotency_key        VARCHAR(64)  NOT NULL,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_transfer_idempotency UNIQUE (origin_account_id, idempotency_key),
    CONSTRAINT ck_transfer_distinct_accounts CHECK (origin_account_id <> destination_account_id)
);

-- Lançamentos do extrato. Uma transferência gera dois lançamentos (enviado e recebido).
CREATE TABLE transactions (
    id           UUID PRIMARY KEY,
    account_id   UUID         NOT NULL REFERENCES account (id),
    description  VARCHAR(120) NOT NULL,
    amount_cents BIGINT       NOT NULL CHECK (amount_cents > 0),
    category     VARCHAR(30)  NOT NULL,
    type         VARCHAR(30)  NOT NULL,
    occurred_at  DATE         NOT NULL,
    -- Momento exato do lançamento (o extrato mostra a hora). Opcional: lançamentos sem hora mostram só a data.
    created_at   TIMESTAMPTZ,
    transfer_id  UUID         REFERENCES transfers (id)
);

CREATE INDEX idx_transactions_account_date ON transactions (account_id, occurred_at);
CREATE INDEX idx_transactions_transfer ON transactions (transfer_id);

-- Proteções de uso ---------------------------------------------------------------------------------
-- Buscas de destinatário por conta (enumeração de usuários).
CREATE TABLE lookup_guard (
    account_id    UUID PRIMARY KEY REFERENCES account (id),
    attempts      INT         NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    blocked_until TIMESTAMPTZ
);

-- Falhas de login por e-mail (chave "email:...").
CREATE TABLE failure_counter (
    counter_key   VARCHAR(200) PRIMARY KEY,
    failures      INT         NOT NULL DEFAULT 0 CHECK (failures >= 0),
    window_start  TIMESTAMPTZ,
    blocked_until TIMESTAMPTZ
);

-- Sessões ------------------------------------------------------------------------------------------
-- Só o hash SHA-256 do refresh token é guardado. A família agrupa os tokens de uma mesma sessão.
CREATE TABLE refresh_token (
    token_hash  VARCHAR(64) PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES app_user (id),
    family_id   UUID        NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL,
    used_at     TIMESTAMPTZ,
    revoked_at  TIMESTAMPTZ
);

CREATE INDEX idx_refresh_token_family ON refresh_token (family_id);

-- Auditoria ----------------------------------------------------------------------------------------
-- Sem chaves estrangeiras de propósito: a gravação de falhas roda em transação própria e não pode
-- esperar locks da transação de negócio. Os identificadores são apenas referências.
CREATE TABLE audit_event (
    id            UUID PRIMARY KEY,
    occurred_at   TIMESTAMPTZ NOT NULL,
    event_type    VARCHAR(40) NOT NULL,
    outcome       VARCHAR(10) NOT NULL,
    actor_user_id UUID,
    account_id    UUID,
    subject_hash  VARCHAR(64),
    reference     VARCHAR(64),
    reason        VARCHAR(60)
);

CREATE INDEX idx_audit_actor_time ON audit_event (actor_user_id, occurred_at);
CREATE INDEX idx_audit_account_time ON audit_event (account_id, occurred_at);

-- Trilha imutável: qualquer UPDATE ou DELETE é recusado pelo banco, mesmo para quem usa a aplicação.
CREATE FUNCTION audit_event_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_event é append-only: UPDATE e DELETE não são permitidos';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_event_append_only
    BEFORE UPDATE OR DELETE ON audit_event
    FOR EACH ROW EXECUTE FUNCTION audit_event_append_only();
