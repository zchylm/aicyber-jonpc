UPDATE reward_programs
SET max_liability_cents = 2500000, updated_at = CURRENT_TIMESTAMP
WHERE code IN ('JON_FOUNDERS_CASHBACK', 'JON_QUEUE_REWARDS');
