-- Record the confirmed opening stock only for variants whose inventory has not been initialized.
WITH opening_stock AS (
    UPDATE system_inventory_balances b
    SET on_hand_quantity = 5, version = b.version + 1, updated_at = CURRENT_TIMESTAMP
    FROM system_builds s
    WHERE s.id = b.system_build_id
      AND s.code IN ('JON-STK-5060-32GB', 'JON-STK-5060TI-32GB', 'JON-STM-5070-32GB', 'JON-STM-5070TI-32GB')
      AND b.version = 0
      AND b.on_hand_quantity = 0
    RETURNING b.system_build_id, s.code
), receipts AS (
    INSERT INTO system_inventory_movements
        (id, system_build_id, movement_type, quantity_delta, quantity_after, reason, idempotency_key)
    SELECT gen_random_uuid(), system_build_id, 'RECEIPT', 5, 5,
           'Confirmed opening stock for 32GB gaming systems', 'opening-stock-2026-11-18:' || code
    FROM opening_stock
    RETURNING system_build_id
)
UPDATE system_builds s
SET sale_enabled = s.preview_enabled, planned_preorder_quantity = 5, updated_at = CURRENT_TIMESTAMP
FROM receipts
WHERE s.id = receipts.system_build_id;
