CREATE TABLE system_inventory_balances (
    system_build_id UUID PRIMARY KEY REFERENCES system_builds(id) ON DELETE CASCADE,
    on_hand_quantity INTEGER NOT NULL DEFAULT 0,
    reorder_point INTEGER NOT NULL DEFAULT 1,
    version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT system_inventory_balances_quantity_check CHECK (on_hand_quantity >= 0 AND reorder_point >= 0)
);

CREATE TABLE system_inventory_movements (
    id UUID PRIMARY KEY,
    system_build_id UUID NOT NULL REFERENCES system_builds(id),
    movement_type VARCHAR(20) NOT NULL,
    quantity_delta INTEGER NOT NULL,
    quantity_after INTEGER NOT NULL,
    reason VARCHAR(300) NOT NULL,
    performed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    idempotency_key VARCHAR(120) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT system_inventory_movements_type_check CHECK (movement_type IN ('RECEIPT', 'ADJUSTMENT')),
    CONSTRAINT system_inventory_movements_delta_check CHECK (quantity_delta <> 0),
    CONSTRAINT system_inventory_movements_after_check CHECK (quantity_after >= 0)
);

CREATE INDEX system_inventory_movements_build_created_idx
    ON system_inventory_movements (system_build_id, created_at DESC);

INSERT INTO system_inventory_balances (system_build_id, on_hand_quantity)
SELECT id, COALESCE(planned_preorder_quantity, 0)
FROM system_builds;
