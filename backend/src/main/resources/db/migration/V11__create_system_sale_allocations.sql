-- Paid system allocations are a saleable pre-order limit, not warehouse on-hand stock.
CREATE TABLE system_sale_allocations (
    sales_order_id UUID PRIMARY KEY REFERENCES sales_orders(id) ON DELETE CASCADE,
    system_build_id UUID NOT NULL REFERENCES system_builds(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX system_sale_allocations_build_idx ON system_sale_allocations (system_build_id);
