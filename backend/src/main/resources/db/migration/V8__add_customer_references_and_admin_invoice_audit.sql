ALTER TABLE users ADD COLUMN customer_reference VARCHAR(24);

UPDATE users
SET customer_reference = 'JON-CUS-' || UPPER(SUBSTRING(REPLACE(id::text, '-', '') FROM 1 FOR 12));

ALTER TABLE users
    ALTER COLUMN customer_reference SET DEFAULT
        ('JON-CUS-' || UPPER(SUBSTRING(REPLACE(gen_random_uuid()::text, '-', '') FROM 1 FOR 12))),
    ALTER COLUMN customer_reference SET NOT NULL,
    ADD CONSTRAINT users_customer_reference_unique UNIQUE (customer_reference),
    ADD CONSTRAINT users_customer_reference_format_check
        CHECK (customer_reference ~ '^JON-CUS-[0-9A-F]{12}$');

ALTER TABLE sales_invoices ADD COLUMN buyer_customer_reference VARCHAR(24);

UPDATE sales_invoices i
SET buyer_customer_reference = u.customer_reference
FROM sales_orders s
JOIN users u ON u.id = s.user_id
WHERE s.id = i.sales_order_id;

ALTER TABLE sales_invoices
    ALTER COLUMN buyer_customer_reference SET NOT NULL;

CREATE TABLE admin_audit_events (
    id UUID PRIMARY KEY,
    admin_user_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(60) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT admin_audit_events_action_check CHECK (
        action IN ('INVOICE_DOWNLOADED', 'INVOICE_RESENT', 'INVOICE_RESEND_FAILED')
    )
);

CREATE INDEX admin_audit_events_entity_idx
    ON admin_audit_events (entity_type, entity_id, created_at DESC);

CREATE INDEX admin_audit_events_admin_idx
    ON admin_audit_events (admin_user_id, created_at DESC);
