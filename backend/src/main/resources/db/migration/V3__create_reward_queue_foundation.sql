CREATE TABLE sales_orders (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    build_request_id UUID UNIQUE REFERENCES build_requests(id) ON DELETE SET NULL,
    order_reference VARCHAR(40) NOT NULL UNIQUE,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    currency CHAR(3) NOT NULL DEFAULT 'AUD',
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING_PAYMENT',
    paid_at TIMESTAMPTZ,
    reward_eligible_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sales_orders_currency_check CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT sales_orders_status_check CHECK (
        status IN ('PENDING_PAYMENT', 'PAID', 'REWARD_ELIGIBLE', 'CANCELLED', 'REFUNDED')
    ),
    CONSTRAINT sales_orders_eligible_at_check CHECK (
        status <> 'REWARD_ELIGIBLE' OR reward_eligible_at IS NOT NULL
    )
);

CREATE INDEX sales_orders_user_created_idx ON sales_orders (user_id, created_at DESC);

CREATE TABLE reward_programs (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'AUD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    next_queue_sequence BIGINT NOT NULL DEFAULT 1 CHECK (next_queue_sequence > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reward_programs_currency_check CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT reward_programs_status_check CHECK (status IN ('ACTIVE', 'PAUSED', 'CLOSED'))
);

CREATE TABLE reward_policy_versions (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES reward_programs(id),
    version INTEGER NOT NULL CHECK (version > 0),
    calculation_type VARCHAR(40) NOT NULL,
    rate_basis_points INTEGER,
    fixed_amount_cents BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reward_policy_versions_program_version_unique UNIQUE (program_id, version),
    CONSTRAINT reward_policy_versions_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    CONSTRAINT reward_policy_versions_dates_check CHECK (
        effective_to IS NULL OR effective_to > effective_from
    ),
    CONSTRAINT reward_policy_versions_calculation_check CHECK (
        (
            calculation_type = 'ORDER_TOTAL_PERCENT'
            AND rate_basis_points BETWEEN 1 AND 10000
            AND fixed_amount_cents IS NULL
        )
        OR
        (
            calculation_type = 'FIXED_AMOUNT'
            AND rate_basis_points IS NULL
            AND fixed_amount_cents > 0
        )
    )
);

CREATE UNIQUE INDEX reward_policy_versions_one_active_idx
    ON reward_policy_versions (program_id)
    WHERE status = 'ACTIVE';

CREATE TABLE reward_inbox_events (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES reward_programs(id),
    external_event_id VARCHAR(120) NOT NULL,
    event_sequence BIGINT GENERATED ALWAYS AS IDENTITY,
    event_type VARCHAR(40) NOT NULL,
    order_id UUID NOT NULL REFERENCES sales_orders(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMPTZ,
    CONSTRAINT reward_inbox_events_external_unique UNIQUE (program_id, external_event_id),
    CONSTRAINT reward_inbox_events_order_type_unique UNIQUE (program_id, order_id, event_type),
    CONSTRAINT reward_inbox_events_type_check CHECK (event_type = 'ORDER_REWARD_ELIGIBLE'),
    CONSTRAINT reward_inbox_events_status_check CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),
    CONSTRAINT reward_inbox_events_processed_at_check CHECK (
        status <> 'COMPLETED' OR processed_at IS NOT NULL
    )
);

CREATE INDEX reward_inbox_events_pending_idx
    ON reward_inbox_events (program_id, event_sequence)
    WHERE status = 'PENDING';

CREATE TABLE reward_queue_entries (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES reward_programs(id),
    order_id UUID NOT NULL REFERENCES sales_orders(id),
    queue_sequence BIGINT NOT NULL CHECK (queue_sequence > 0),
    target_amount_cents BIGINT NOT NULL CHECK (target_amount_cents > 0),
    allocated_amount_cents BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'WAITING',
    joined_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT reward_queue_entries_program_order_unique UNIQUE (program_id, order_id),
    CONSTRAINT reward_queue_entries_program_sequence_unique UNIQUE (program_id, queue_sequence),
    CONSTRAINT reward_queue_entries_amount_check CHECK (
        allocated_amount_cents >= 0 AND allocated_amount_cents <= target_amount_cents
    ),
    CONSTRAINT reward_queue_entries_status_check CHECK (status IN ('WAITING', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT reward_queue_entries_completion_check CHECK (
        (
            status = 'COMPLETED'
            AND allocated_amount_cents = target_amount_cents
            AND completed_at IS NOT NULL
        )
        OR
        (
            status <> 'COMPLETED'
            AND completed_at IS NULL
        )
    )
);

CREATE INDEX reward_queue_entries_head_idx
    ON reward_queue_entries (program_id, queue_sequence)
    WHERE status = 'WAITING';

CREATE TABLE reward_contributions (
    id UUID PRIMARY KEY,
    program_id UUID NOT NULL REFERENCES reward_programs(id),
    source_order_id UUID NOT NULL REFERENCES sales_orders(id),
    policy_version_id UUID NOT NULL REFERENCES reward_policy_versions(id),
    original_amount_cents BIGINT NOT NULL CHECK (original_amount_cents > 0),
    remaining_amount_cents BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fully_allocated_at TIMESTAMPTZ,
    CONSTRAINT reward_contributions_program_order_unique UNIQUE (program_id, source_order_id),
    CONSTRAINT reward_contributions_remaining_check CHECK (
        remaining_amount_cents >= 0 AND remaining_amount_cents <= original_amount_cents
    ),
    CONSTRAINT reward_contributions_status_check CHECK (
        (
            status = 'PENDING'
            AND remaining_amount_cents = original_amount_cents
            AND fully_allocated_at IS NULL
        )
        OR
        (
            status = 'PARTIALLY_ALLOCATED'
            AND remaining_amount_cents > 0
            AND remaining_amount_cents < original_amount_cents
            AND fully_allocated_at IS NULL
        )
        OR
        (
            status = 'ALLOCATED'
            AND remaining_amount_cents = 0
            AND fully_allocated_at IS NOT NULL
        )
    )
);

CREATE INDEX reward_contributions_pending_idx
    ON reward_contributions (program_id, created_at)
    WHERE status IN ('PENDING', 'PARTIALLY_ALLOCATED');

CREATE TABLE reward_allocations (
    id UUID PRIMARY KEY,
    contribution_id UUID NOT NULL REFERENCES reward_contributions(id),
    recipient_queue_entry_id UUID NOT NULL REFERENCES reward_queue_entries(id),
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT reward_allocations_contribution_recipient_unique UNIQUE (
        contribution_id,
        recipient_queue_entry_id
    )
);

CREATE INDEX reward_allocations_recipient_created_idx
    ON reward_allocations (recipient_queue_entry_id, created_at);
