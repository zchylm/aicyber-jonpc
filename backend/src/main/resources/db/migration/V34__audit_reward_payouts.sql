ALTER TABLE admin_audit_events
    DROP CONSTRAINT admin_audit_events_action_check;

ALTER TABLE admin_audit_events
    ADD CONSTRAINT admin_audit_events_action_check CHECK (
        action IN ('INVOICE_DOWNLOADED', 'INVOICE_RESENT', 'INVOICE_RESEND_FAILED', 'REWARD_PAID_RECORDED')
    );
