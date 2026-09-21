CREATE TABLE component_categories (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT component_categories_code_check CHECK (code ~ '^[A-Z][A-Z0-9_]*$'),
    CONSTRAINT component_categories_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

INSERT INTO component_categories (id, code, name, sort_order) VALUES
    ('10000000-0000-0000-0000-000000000001', 'CPU', 'Processors', 10),
    ('10000000-0000-0000-0000-000000000002', 'GPU', 'Graphics cards', 20),
    ('10000000-0000-0000-0000-000000000003', 'MOTHERBOARD', 'Motherboards', 30),
    ('10000000-0000-0000-0000-000000000004', 'MEMORY', 'Memory', 40),
    ('10000000-0000-0000-0000-000000000005', 'STORAGE', 'Storage', 50),
    ('10000000-0000-0000-0000-000000000006', 'PSU', 'Power supplies', 60),
    ('10000000-0000-0000-0000-000000000007', 'CASE', 'Cases', 70),
    ('10000000-0000-0000-0000-000000000008', 'COOLING', 'Cooling', 80);

CREATE TABLE inventory_items (
    id UUID PRIMARY KEY,
    category_id UUID NOT NULL REFERENCES component_categories(id),
    sku VARCHAR(80) NOT NULL UNIQUE,
    brand VARCHAR(100) NOT NULL,
    model VARCHAR(160) NOT NULL,
    display_name VARCHAR(220) NOT NULL,
    description TEXT,
    specifications JSONB NOT NULL DEFAULT '{}'::jsonb,
    unit_cost_cents BIGINT CHECK (unit_cost_cents IS NULL OR unit_cost_cents >= 0),
    retail_price_cents BIGINT NOT NULL CHECK (retail_price_cents >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'AUD',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_items_sku_check CHECK (sku ~ '^[A-Z0-9][A-Z0-9._-]*$'),
    CONSTRAINT inventory_items_currency_check CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT inventory_items_specifications_check CHECK (jsonb_typeof(specifications) = 'object'),
    CONSTRAINT inventory_items_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'DISCONTINUED'))
);

CREATE INDEX inventory_items_category_status_idx ON inventory_items (category_id, status);
CREATE INDEX inventory_items_name_search_idx ON inventory_items (LOWER(display_name));

CREATE TABLE inventory_locations (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    location_type VARCHAR(30) NOT NULL DEFAULT 'WAREHOUSE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_locations_code_check CHECK (code ~ '^[A-Z0-9][A-Z0-9_-]*$'),
    CONSTRAINT inventory_locations_type_check CHECK (location_type IN ('WAREHOUSE', 'STORE', 'SERVICE')),
    CONSTRAINT inventory_locations_status_check CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE TABLE system_builds (
    id UUID PRIMARY KEY,
    code VARCHAR(60) NOT NULL UNIQUE,
    name VARCHAR(160) NOT NULL,
    direction VARCHAR(40) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT system_builds_code_check CHECK (code ~ '^[A-Z0-9][A-Z0-9_-]*$'),
    CONSTRAINT system_builds_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE system_build_versions (
    id UUID PRIMARY KEY,
    system_build_id UUID NOT NULL REFERENCES system_builds(id),
    version INTEGER NOT NULL CHECK (version > 0),
    display_price_cents BIGINT NOT NULL CHECK (display_price_cents >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'AUD',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    effective_from TIMESTAMPTZ,
    effective_to TIMESTAMPTZ,
    created_by UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT system_build_versions_unique UNIQUE (system_build_id, version),
    CONSTRAINT system_build_versions_currency_check CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT system_build_versions_status_check CHECK (status IN ('DRAFT', 'ACTIVE', 'RETIRED')),
    CONSTRAINT system_build_versions_dates_check CHECK (effective_to IS NULL OR effective_from IS NOT NULL AND effective_to > effective_from)
);

CREATE UNIQUE INDEX system_build_versions_one_active_idx
    ON system_build_versions (system_build_id) WHERE status = 'ACTIVE';

CREATE TABLE system_build_components (
    id UUID PRIMARY KEY,
    build_version_id UUID NOT NULL REFERENCES system_build_versions(id),
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    component_role VARCHAR(60) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1 CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT system_build_components_unique UNIQUE (build_version_id, component_role, inventory_item_id)
);

ALTER TABLE sales_orders
    ADD COLUMN source_build_version_id UUID REFERENCES system_build_versions(id);

CREATE TABLE sales_order_items (
    id UUID PRIMARY KEY,
    sales_order_id UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    sku_snapshot VARCHAR(80) NOT NULL,
    name_snapshot VARCHAR(220) NOT NULL,
    unit_price_cents BIGINT NOT NULL CHECK (unit_price_cents >= 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    line_total_cents BIGINT GENERATED ALWAYS AS (unit_price_cents * quantity) STORED,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sales_order_items_unique UNIQUE (sales_order_id, inventory_item_id)
);

CREATE INDEX sales_order_items_order_idx ON sales_order_items (sales_order_id);

CREATE TABLE inventory_reservations (
    id UUID PRIMARY KEY,
    sales_order_id UUID NOT NULL UNIQUE REFERENCES sales_orders(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    released_at TIMESTAMPTZ,
    committed_at TIMESTAMPTZ,
    CONSTRAINT inventory_reservations_status_check CHECK (status IN ('ACTIVE', 'COMMITTED', 'RELEASED', 'EXPIRED')),
    CONSTRAINT inventory_reservations_expiry_check CHECK (expires_at > created_at),
    CONSTRAINT inventory_reservations_completion_check CHECK (
        (status = 'ACTIVE' AND released_at IS NULL AND committed_at IS NULL)
        OR (status = 'COMMITTED' AND committed_at IS NOT NULL AND released_at IS NULL)
        OR (status IN ('RELEASED', 'EXPIRED') AND released_at IS NOT NULL AND committed_at IS NULL)
    )
);

CREATE INDEX inventory_reservations_active_expiry_idx
    ON inventory_reservations (expires_at) WHERE status = 'ACTIVE';

CREATE TABLE inventory_reservation_items (
    id UUID PRIMARY KEY,
    reservation_id UUID NOT NULL REFERENCES inventory_reservations(id) ON DELETE CASCADE,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    location_id UUID NOT NULL REFERENCES inventory_locations(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_reservation_items_unique UNIQUE (reservation_id, inventory_item_id, location_id)
);

CREATE TABLE inventory_balances (
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    location_id UUID NOT NULL REFERENCES inventory_locations(id),
    on_hand_quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    reorder_point INTEGER NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (inventory_item_id, location_id),
    CONSTRAINT inventory_balances_quantity_check CHECK (
        on_hand_quantity >= 0
        AND reserved_quantity >= 0
        AND reserved_quantity <= on_hand_quantity
        AND reorder_point >= 0
    )
);

CREATE INDEX inventory_balances_location_idx ON inventory_balances (location_id);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY,
    inventory_item_id UUID NOT NULL REFERENCES inventory_items(id),
    location_id UUID NOT NULL REFERENCES inventory_locations(id),
    movement_type VARCHAR(30) NOT NULL,
    on_hand_delta INTEGER NOT NULL DEFAULT 0,
    reserved_delta INTEGER NOT NULL DEFAULT 0,
    on_hand_after INTEGER NOT NULL,
    reserved_after INTEGER NOT NULL,
    sales_order_id UUID REFERENCES sales_orders(id),
    reservation_id UUID REFERENCES inventory_reservations(id),
    reason VARCHAR(300),
    performed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT inventory_movements_type_check CHECK (
        movement_type IN ('RECEIPT', 'ADJUSTMENT', 'RESERVATION', 'RELEASE', 'FULFILMENT', 'RETURN')
    ),
    CONSTRAINT inventory_movements_delta_check CHECK (on_hand_delta <> 0 OR reserved_delta <> 0),
    CONSTRAINT inventory_movements_after_check CHECK (
        on_hand_after >= 0 AND reserved_after >= 0 AND reserved_after <= on_hand_after
    )
);

CREATE INDEX inventory_movements_item_location_created_idx
    ON inventory_movements (inventory_item_id, location_id, created_at DESC);
CREATE INDEX inventory_movements_order_idx
    ON inventory_movements (sales_order_id) WHERE sales_order_id IS NOT NULL;
