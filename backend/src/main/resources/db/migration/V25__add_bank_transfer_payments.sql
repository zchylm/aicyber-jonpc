ALTER TABLE payments DROP CONSTRAINT payments_provider_check;
ALTER TABLE payments ADD CONSTRAINT payments_provider_check
    CHECK (provider IN ('MOCK', 'STRIPE', 'BANK_TRANSFER'));

ALTER TABLE payments
    ADD COLUMN expires_at TIMESTAMPTZ,
    ADD COLUMN customer_reported_at TIMESTAMPTZ,
    ADD COLUMN confirmed_by UUID REFERENCES users(id),
    ADD COLUMN confirmed_at TIMESTAMPTZ,
    ADD COLUMN review_note VARCHAR(500);

CREATE INDEX payments_bank_review_queue_idx
    ON payments (customer_reported_at, created_at)
    WHERE provider = 'BANK_TRANSFER' AND status = 'PROCESSING';

CREATE INDEX payments_bank_expiry_idx
    ON payments (expires_at)
    WHERE provider = 'BANK_TRANSFER' AND status = 'CREATED';
