ALTER TABLE reward_commitments
    DROP CONSTRAINT reward_commitments_status_check;

ALTER TABLE reward_commitments
    ADD COLUMN payout_due_at TIMESTAMPTZ,
    ADD COLUMN processing_at TIMESTAMPTZ,
    ADD COLUMN payout_method VARCHAR(40) NOT NULL DEFAULT 'ORIGINAL_PAYMENT_METHOD',
    ADD COLUMN payout_reference VARCHAR(120),
    ADD COLUMN payout_failure_reason VARCHAR(500),
    ADD CONSTRAINT reward_commitments_status_check
        CHECK (status IN ('LOCKED', 'PAYABLE', 'PROCESSING', 'PAID', 'FAILED', 'VOID'));

ALTER TABLE sales_invoices
    ADD COLUMN founder_number INTEGER,
    ADD COLUMN founder_tier_name VARCHAR(80),
    ADD COLUMN founder_cashback_amount_cents BIGINT,
    ADD COLUMN founder_cashback_terms VARCHAR(240),
    ADD CONSTRAINT sales_invoices_founder_snapshot_check CHECK (
        (founder_number IS NULL AND founder_tier_name IS NULL AND founder_cashback_amount_cents IS NULL)
        OR
        (founder_number > 0 AND founder_tier_name IS NOT NULL AND founder_cashback_amount_cents > 0)
    );
