UPDATE reward_founder_tiers t
SET cap_cents = 50000
FROM reward_programs p
WHERE t.program_id = p.id
  AND p.code = 'JON_FOUNDERS_CASHBACK';

UPDATE reward_programs
SET max_liability_cents = 2500000,
    updated_at = CURRENT_TIMESTAMP
WHERE code = 'JON_FOUNDERS_CASHBACK';
