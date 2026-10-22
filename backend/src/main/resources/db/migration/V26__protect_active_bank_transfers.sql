CREATE UNIQUE INDEX payments_one_active_bank_transfer_per_order_idx
    ON payments (order_id)
    WHERE provider = 'BANK_TRANSFER' AND status IN ('CREATED', 'PROCESSING');
