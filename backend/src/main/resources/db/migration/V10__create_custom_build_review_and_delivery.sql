CREATE TABLE custom_build_quotes (
    id UUID PRIMARY KEY,
    build_request_id UUID NOT NULL REFERENCES build_requests(id),
    version INTEGER NOT NULL CHECK (version > 0),
    status VARCHAR(20) NOT NULL CHECK (status IN ('SENT', 'ACCEPTED', 'SUPERSEDED')),
    total_cents BIGINT NOT NULL CHECK (total_cents > 0),
    currency CHAR(3) NOT NULL DEFAULT 'AUD',
    reviewer_id UUID NOT NULL REFERENCES users(id),
    review_note TEXT,
    valid_until TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    accepted_at TIMESTAMPTZ,
    UNIQUE (build_request_id, version)
);

CREATE UNIQUE INDEX custom_build_quotes_active_request_idx
    ON custom_build_quotes (build_request_id) WHERE status IN ('SENT', 'ACCEPTED');
CREATE INDEX custom_build_quotes_request_version_idx
    ON custom_build_quotes (build_request_id, version DESC);

CREATE TABLE build_delivery_details (
    build_request_id UUID PRIMARY KEY REFERENCES build_requests(id) ON DELETE CASCADE,
    recipient_name VARCHAR(160) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    address_line_1 VARCHAR(200) NOT NULL,
    address_line_2 VARCHAR(200),
    suburb VARCHAR(100) NOT NULL,
    state VARCHAR(20) NOT NULL,
    postcode VARCHAR(12) NOT NULL,
    country CHAR(2) NOT NULL DEFAULT 'AU',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
