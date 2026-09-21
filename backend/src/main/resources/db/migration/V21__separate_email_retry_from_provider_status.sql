ALTER TABLE transactional_email_outbox
    DROP CONSTRAINT transactional_email_outbox_status_check;

ALTER TABLE transactional_email_outbox
    ADD CONSTRAINT transactional_email_outbox_status_check CHECK (
        status IN ('QUEUED', 'SENDING', 'RETRY_PENDING', 'ACCEPTED', 'DELIVERED',
                   'DELAYED', 'FAILED', 'BOUNCED', 'COMPLAINED', 'SUPPRESSED')
    );

DROP INDEX transactional_email_outbox_pending_idx;

CREATE INDEX transactional_email_outbox_pending_idx
    ON transactional_email_outbox (next_attempt_at, queued_at)
    WHERE status IN ('QUEUED', 'RETRY_PENDING');

