ALTER TABLE reward_commitments
    ADD COLUMN reservation_expires_at TIMESTAMPTZ;

ALTER TABLE reward_commitments
    DROP CONSTRAINT reward_commitments_program_month_sequence_unique;

CREATE UNIQUE INDEX reward_commitments_active_month_sequence_unique
    ON reward_commitments (program_id, reward_month, founder_sequence)
    WHERE status <> 'VOID';

UPDATE reward_programs
SET name = 'JON.PC Founder 5% Discount', updated_at = CURRENT_TIMESTAMP
WHERE code IN ('JON_FOUNDERS_CASHBACK', 'JON_QUEUE_REWARDS');

UPDATE reward_founder_tiers
SET display_name = 'Founder 5% Discount', rate_basis_points = 500, cap_cents = 999999999999
WHERE program_id IN (
    SELECT id FROM reward_programs WHERE code IN ('JON_FOUNDERS_CASHBACK', 'JON_QUEUE_REWARDS')
);

ALTER TABLE sales_invoice_lines
    DROP CONSTRAINT sales_invoice_lines_unit_price_ex_gst_cents_check,
    DROP CONSTRAINT sales_invoice_lines_gst_cents_check,
    DROP CONSTRAINT sales_invoice_lines_line_total_cents_check;

ALTER TABLE sales_invoice_lines
    ADD CONSTRAINT sales_invoice_lines_line_total_nonzero_check CHECK (line_total_cents <> 0);
