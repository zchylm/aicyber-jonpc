ALTER TABLE reward_programs
    ADD COLUMN max_positions INTEGER NOT NULL DEFAULT 50 CHECK (max_positions > 0),
    ADD COLUMN max_liability_cents BIGINT NOT NULL DEFAULT 1850000 CHECK (max_liability_cents >= 0);

CREATE TABLE reward_founder_tiers (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES reward_programs(id),
    tier_code VARCHAR(40) NOT NULL,
    display_name VARCHAR(80) NOT NULL,
    position_start INTEGER NOT NULL CHECK (position_start > 0),
    position_end INTEGER NOT NULL CHECK (position_end >= position_start),
    rate_basis_points INTEGER NOT NULL CHECK (rate_basis_points BETWEEN 1 AND 10000),
    cap_cents BIGINT NOT NULL CHECK (cap_cents > 0),
    CONSTRAINT reward_founder_tiers_program_code_unique UNIQUE (program_id, tier_code),
    CONSTRAINT reward_founder_tiers_program_start_unique UNIQUE (program_id, position_start)
);

CREATE TABLE reward_commitments (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES reward_programs(id),
    tier_id UUID NOT NULL REFERENCES reward_founder_tiers(id),
    order_id UUID NOT NULL UNIQUE REFERENCES sales_orders(id),
    user_id UUID NOT NULL REFERENCES users(id),
    founder_sequence INTEGER NOT NULL CHECK (founder_sequence > 0),
    purchase_amount_cents BIGINT NOT NULL CHECK (purchase_amount_cents > 0),
    eligible_spend_cents BIGINT NOT NULL CHECK (eligible_spend_cents > 0),
    rate_basis_points INTEGER NOT NULL CHECK (rate_basis_points BETWEEN 1 AND 10000),
    cap_cents BIGINT NOT NULL CHECK (cap_cents > 0),
    cashback_amount_cents BIGINT NOT NULL CHECK (cashback_amount_cents > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'LOCKED',
    locked_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payable_at TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    voided_at TIMESTAMPTZ,
    CONSTRAINT reward_commitments_program_sequence_unique UNIQUE (program_id, founder_sequence),
    CONSTRAINT reward_commitments_program_user_unique UNIQUE (program_id, user_id),
    CONSTRAINT reward_commitments_status_check CHECK (status IN ('LOCKED', 'PAYABLE', 'PAID', 'VOID')),
    CONSTRAINT reward_commitments_amount_check CHECK (cashback_amount_cents <= cap_cents)
);

CREATE INDEX reward_commitments_program_locked_idx
    ON reward_commitments (program_id, founder_sequence);

UPDATE reward_programs
SET name = 'JON. PC Founders Cashback', max_positions = 50, max_liability_cents = 1850000,
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'JON_QUEUE_REWARDS';

INSERT INTO reward_founder_tiers
    (id, program_id, tier_code, display_name, position_start, position_end, rate_basis_points, cap_cents)
SELECT gen_random_uuid(), p.id, tier.tier_code, tier.display_name, tier.position_start,
       tier.position_end, tier.rate_basis_points, tier.cap_cents
FROM reward_programs p
CROSS JOIN (VALUES
    ('LAUNCH', 'Launch Founder', 1, 10, 1500, 50000),
    ('EARLY', 'Early Founder', 11, 25, 1200, 40000),
    ('FOUNDER', 'Founder', 26, 50, 1000, 30000)
) AS tier(tier_code, display_name, position_start, position_end, rate_basis_points, cap_cents)
ON CONFLICT (program_id, tier_code) DO NOTHING;
