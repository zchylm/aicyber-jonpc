CREATE TABLE payments (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL REFERENCES sales_orders(id),
    payment_reference VARCHAR(40) NOT NULL UNIQUE,
    provider VARCHAR(30) NOT NULL,
    provider_payment_id VARCHAR(160),
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    currency CHAR(3) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
    idempotency_key VARCHAR(120) NOT NULL,
    failure_reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    succeeded_at TIMESTAMPTZ,
    CONSTRAINT payments_order_idempotency_unique UNIQUE (order_id, idempotency_key),
    CONSTRAINT payments_provider_payment_unique UNIQUE (provider, provider_payment_id),
    CONSTRAINT payments_currency_check CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT payments_provider_check CHECK (provider IN ('MOCK', 'STRIPE')),
    CONSTRAINT payments_status_check CHECK (status IN ('CREATED', 'PROCESSING', 'SUCCEEDED', 'FAILED', 'CANCELLED')),
    CONSTRAINT payments_result_check CHECK (
        (status = 'SUCCEEDED' AND succeeded_at IS NOT NULL AND failure_reason IS NULL)
        OR (status = 'FAILED' AND succeeded_at IS NULL AND failure_reason IS NOT NULL)
        OR (status IN ('CREATED', 'PROCESSING', 'CANCELLED') AND succeeded_at IS NULL)
    )
);

CREATE UNIQUE INDEX payments_one_success_per_order_idx
    ON payments (order_id)
    WHERE status = 'SUCCEEDED';

CREATE INDEX payments_order_created_idx ON payments (order_id, created_at DESC);
