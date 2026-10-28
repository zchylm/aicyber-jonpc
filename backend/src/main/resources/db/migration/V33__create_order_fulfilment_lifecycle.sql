CREATE TABLE order_fulfilments (
    sales_order_id UUID PRIMARY KEY REFERENCES sales_orders(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL DEFAULT 'PREPARING',
    carrier VARCHAR(120),
    tracking_number VARCHAR(160),
    dispatched_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    auto_confirm_at TIMESTAMPTZ,
    received_at TIMESTAMPTZ,
    issue_reported_at TIMESTAMPTZ,
    issue_summary VARCHAR(500),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT order_fulfilments_status_check CHECK (
        status IN ('PREPARING', 'DISPATCHED', 'DELIVERED', 'ISSUE_REPORTED', 'RECEIVED')
    ),
    CONSTRAINT order_fulfilments_delivery_check CHECK (
        status NOT IN ('DELIVERED', 'ISSUE_REPORTED', 'RECEIVED') OR delivered_at IS NOT NULL
    ),
    CONSTRAINT order_fulfilments_received_check CHECK (
        status <> 'RECEIVED' OR received_at IS NOT NULL
    )
);

INSERT INTO order_fulfilments (sales_order_id)
SELECT id FROM sales_orders WHERE status IN ('PAID', 'REWARD_ELIGIBLE');

CREATE INDEX order_fulfilments_auto_confirm_idx
    ON order_fulfilments (auto_confirm_at)
    WHERE status = 'DELIVERED';
