ALTER TABLE users
    ADD COLUMN email_verified_at TIMESTAMPTZ,
    ADD COLUMN auth_version INTEGER NOT NULL DEFAULT 1;

-- Accounts created before email verification existed remain usable.
UPDATE users
SET email_verified_at = created_at
WHERE email_verified_at IS NULL;

CREATE TABLE account_action_tokens (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    purpose VARCHAR(40) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT account_action_tokens_hash_unique UNIQUE (token_hash),
    CONSTRAINT account_action_tokens_purpose_check
        CHECK (purpose IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET')),
    CONSTRAINT account_action_tokens_expiry_check CHECK (expires_at > created_at),
    CONSTRAINT account_action_tokens_completion_check
        CHECK (consumed_at IS NULL OR revoked_at IS NULL)
);

CREATE UNIQUE INDEX account_action_tokens_one_active_idx
    ON account_action_tokens (user_id, purpose)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;

CREATE INDEX account_action_tokens_user_idx
    ON account_action_tokens (user_id, purpose, created_at DESC);

CREATE INDEX account_action_tokens_expiry_idx
    ON account_action_tokens (expires_at)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;
