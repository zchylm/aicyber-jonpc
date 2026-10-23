ALTER TABLE payments
    ADD COLUMN provider_checkout_session_id VARCHAR(255);

CREATE UNIQUE INDEX payments_stripe_checkout_session_unique
    ON payments (provider_checkout_session_id)
    WHERE provider_checkout_session_id IS NOT NULL;

CREATE UNIQUE INDEX payments_one_active_stripe_per_order_idx
    ON payments (order_id)
    WHERE provider = 'STRIPE' AND status IN ('CREATED', 'PROCESSING');
