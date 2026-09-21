DROP INDEX transactional_email_outbox_pending_idx;

CREATE INDEX transactional_email_outbox_pending_idx
    ON transactional_email_outbox (next_attempt_at, queued_at)
    WHERE status IN ('QUEUED', 'RETRY_PENDING', 'SENDING');

