-- SyncBridge Demo Backend — Database Schema
-- Runs automatically on first PostgreSQL container boot (docker-entrypoint-initdb.d)

-- ─── Orders ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS orders (
    id              UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    transaction_id  UUID            NOT NULL UNIQUE,
    customer_name   TEXT            NOT NULL,
    product_name    TEXT            NOT NULL,
    quantity        INTEGER         NOT NULL CHECK (quantity > 0),
    total_amount    NUMERIC(10, 2)  NOT NULL,
    status          TEXT            NOT NULL DEFAULT 'CONFIRMED',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_transaction_id ON orders(transaction_id);

-- ─── Idempotency Log ──────────────────────────────────────────────────────────
-- transaction_id is the PRIMARY KEY — INSERT ON CONFLICT DO NOTHING gives atomic
-- race-safe claim semantics without any application-level locking.
CREATE TABLE IF NOT EXISTS idempotency_log (
    transaction_id      UUID        PRIMARY KEY,
    status              TEXT        NOT NULL,       -- PROCESSING | SUCCESS | CONFLICT
    response_status     INTEGER     NOT NULL,       -- HTTP code (0 while PROCESSING)
    response_body       JSONB       NOT NULL,
    endpoint            TEXT        NOT NULL,       -- e.g. 'POST /api/orders'
    client_timestamp    BIGINT,                     -- X-Client-Timestamp (epoch ms)
    attempt_count       INTEGER,                    -- X-Attempt-Count from first request
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ NOT NULL DEFAULT (NOW() + INTERVAL '30 days')
);

-- Index for efficient TTL cleanup queries
CREATE INDEX IF NOT EXISTS idx_idempotency_log_expires_at ON idempotency_log(expires_at);
