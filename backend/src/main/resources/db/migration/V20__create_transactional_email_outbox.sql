CREATE TABLE transactional_email_outbox (
    id UUID PRIMARY KEY,
    message_type VARCHAR(60) NOT NULL,
    recipient_email VARCHAR(320) NOT NULL,
    recipient_name VARCHAR(200),
    subject VARCHAR(240) NOT NULL,
    text_body TEXT,
    html_body TEXT,
    attachment_filename VARCHAR(240),
    attachment_content_type VARCHAR(120),
    attachment_content BYTEA,
    aggregate_type VARCHAR(60),
    aggregate_id UUID,
    idempotency_key VARCHAR(200) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL DEFAULT 'QUEUED',
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    provider_message_id VARCHAR(200) UNIQUE,
    last_error VARCHAR(500),
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    queued_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempted_at TIMESTAMPTZ,
    accepted_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT transactional_email_outbox_status_check CHECK (
        status IN ('QUEUED', 'SENDING', 'ACCEPTED', 'DELIVERED', 'DELAYED', 'FAILED', 'BOUNCED', 'COMPLAINED', 'SUPPRESSED')
    ),
    CONSTRAINT transactional_email_outbox_body_check CHECK (
        status IN ('ACCEPTED', 'DELIVERED', 'DELAYED', 'FAILED', 'BOUNCED', 'COMPLAINED', 'SUPPRESSED')
        OR text_body IS NOT NULL OR html_body IS NOT NULL
    ),
    CONSTRAINT transactional_email_outbox_attachment_check CHECK (
        (attachment_filename IS NULL AND attachment_content_type IS NULL AND attachment_content IS NULL)
        OR (attachment_filename IS NOT NULL AND attachment_content_type IS NOT NULL AND attachment_content IS NOT NULL)
    )
);

CREATE INDEX transactional_email_outbox_pending_idx
    ON transactional_email_outbox (next_attempt_at, queued_at)
    WHERE status IN ('QUEUED', 'FAILED', 'DELAYED');

CREATE INDEX transactional_email_outbox_aggregate_idx
    ON transactional_email_outbox (aggregate_type, aggregate_id)
    WHERE aggregate_id IS NOT NULL;

CREATE TABLE transactional_email_webhook_events (
    event_id VARCHAR(200) PRIMARY KEY,
    event_type VARCHAR(80) NOT NULL,
    provider_message_id VARCHAR(200),
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
