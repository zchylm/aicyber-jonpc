CREATE TABLE saved_builds (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    direction VARCHAR(40) NOT NULL,
    budget_range VARCHAR(80),
    estimated_price INTEGER NOT NULL CHECK (estimated_price >= 0),
    recommended_baseline INTEGER NOT NULL CHECK (recommended_baseline >= 0),
    selected_adjustments INTEGER NOT NULL DEFAULT 0,
    configuration_snapshot JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT saved_builds_status_check CHECK (status IN ('ACTIVE', 'ARCHIVED'))
);

CREATE INDEX saved_builds_user_updated_idx ON saved_builds (user_id, updated_at DESC);

CREATE TABLE build_requests (
    id UUID PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    saved_build_id UUID REFERENCES saved_builds(id) ON DELETE SET NULL,
    request_reference VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    email VARCHAR(320) NOT NULL,
    phone VARCHAR(40),
    location VARCHAR(160) NOT NULL,
    notes TEXT,
    contact_requested BOOLEAN NOT NULL DEFAULT TRUE,
    direction VARCHAR(40) NOT NULL,
    estimated_price INTEGER NOT NULL CHECK (estimated_price >= 0),
    recommended_baseline INTEGER NOT NULL CHECK (recommended_baseline >= 0),
    selected_adjustments INTEGER NOT NULL DEFAULT 0,
    configuration_snapshot JSONB NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT build_requests_status_check CHECK (status IN ('RECEIVED', 'CONTACTED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX build_requests_user_created_idx ON build_requests (user_id, created_at DESC);
