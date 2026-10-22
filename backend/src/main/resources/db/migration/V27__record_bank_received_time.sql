ALTER TABLE payments
    ADD COLUMN bank_received_at TIMESTAMPTZ;

CREATE INDEX payments_bank_received_order_idx
    ON payments (bank_received_at)
    WHERE provider = 'BANK_TRANSFER' AND status = 'SUCCEEDED';
