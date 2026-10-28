ALTER TABLE sales_orders
    ADD COLUMN product_subtotal_cents BIGINT,
    ADD COLUMN shipping_fee_cents BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN discount_cents BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN refunded_product_cents BIGINT NOT NULL DEFAULT 0;

UPDATE sales_orders SET product_subtotal_cents = amount_cents WHERE product_subtotal_cents IS NULL;

ALTER TABLE sales_orders
    ALTER COLUMN product_subtotal_cents SET NOT NULL,
    ADD CONSTRAINT sales_orders_product_subtotal_check CHECK (product_subtotal_cents > 0),
    ADD CONSTRAINT sales_orders_shipping_fee_check CHECK (shipping_fee_cents >= 0),
    ADD CONSTRAINT sales_orders_discount_check CHECK (discount_cents >= 0 AND discount_cents <= product_subtotal_cents),
    ADD CONSTRAINT sales_orders_refunded_product_check CHECK (
        refunded_product_cents >= 0 AND refunded_product_cents <= product_subtotal_cents - discount_cents
    ),
    ADD CONSTRAINT sales_orders_total_breakdown_check CHECK (
        amount_cents = product_subtotal_cents - discount_cents + shipping_fee_cents
    );

CREATE TABLE reward_months (
    program_id UUID NOT NULL REFERENCES reward_programs(id),
    month_start DATE NOT NULL,
    next_sequence INTEGER NOT NULL DEFAULT 1 CHECK (next_sequence > 0),
    max_positions INTEGER NOT NULL DEFAULT 50 CHECK (max_positions > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (program_id, month_start),
    CONSTRAINT reward_months_first_day_check CHECK (EXTRACT(DAY FROM month_start) = 1)
);

ALTER TABLE reward_commitments
    ADD COLUMN reward_month DATE,
    ADD COLUMN qualifying_at TIMESTAMPTZ;

UPDATE reward_commitments
SET qualifying_at = locked_at,
    reward_month = date_trunc('month', locked_at AT TIME ZONE 'Australia/Melbourne')::date
WHERE qualifying_at IS NULL OR reward_month IS NULL;

ALTER TABLE reward_commitments
    ALTER COLUMN reward_month SET NOT NULL,
    ALTER COLUMN qualifying_at SET NOT NULL,
    DROP CONSTRAINT reward_commitments_program_sequence_unique,
    DROP CONSTRAINT reward_commitments_program_user_unique,
    ADD CONSTRAINT reward_commitments_program_month_sequence_unique
        UNIQUE (program_id, reward_month, founder_sequence);

INSERT INTO reward_months (program_id, month_start, next_sequence, max_positions)
SELECT program_id, reward_month, MAX(founder_sequence) + 1, 50
FROM reward_commitments
GROUP BY program_id, reward_month
ON CONFLICT (program_id, month_start) DO UPDATE SET
    next_sequence = GREATEST(reward_months.next_sequence, EXCLUDED.next_sequence),
    updated_at = CURRENT_TIMESTAMP;

UPDATE reward_programs
SET name = 'JON.PC Monthly Founder Cashback',
    max_positions = 50,
    max_liability_cents = 999999999999,
    updated_at = CURRENT_TIMESTAMP
WHERE code IN ('JON_FOUNDERS_CASHBACK', 'JON_QUEUE_REWARDS');

UPDATE reward_founder_tiers
SET display_name = 'Monthly Founder Cashback',
    rate_basis_points = 500,
    cap_cents = 999999999999
WHERE program_id IN (
    SELECT id FROM reward_programs WHERE code IN ('JON_FOUNDERS_CASHBACK', 'JON_QUEUE_REWARDS')
);

CREATE INDEX reward_commitments_month_status_idx
    ON reward_commitments (program_id, reward_month, status);
