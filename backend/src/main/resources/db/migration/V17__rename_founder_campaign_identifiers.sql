ALTER TABLE reward_programs RENAME COLUMN next_queue_sequence TO next_founder_sequence;

UPDATE reward_programs
SET code = 'JON_FOUNDERS_CASHBACK', updated_at = CURRENT_TIMESTAMP
WHERE code = 'JON_QUEUE_REWARDS';
