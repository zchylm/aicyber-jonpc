-- First live batch: four desktop systems and four compact systems, five units each.
UPDATE system_builds
SET sale_enabled = TRUE,
    planned_preorder_quantity = 5,
    updated_at = CURRENT_TIMESTAMP
WHERE code IN (
    'JON-STK-5060',
    'JON-STK-5060TI',
    'JON-STM-5070',
    'JON-STM-5070TI',
    'JON-EDGE-MINI',
    'JON-EDGE-AI',
    'JON-EDGE-PRO',
    'JON-EDGE-MAX'
);

INSERT INTO system_inventory_balances (system_build_id, on_hand_quantity)
SELECT id, 5
FROM system_builds
WHERE code IN (
    'JON-STK-5060',
    'JON-STK-5060TI',
    'JON-STM-5070',
    'JON-STM-5070TI',
    'JON-EDGE-MINI',
    'JON-EDGE-AI',
    'JON-EDGE-PRO',
    'JON-EDGE-MAX'
)
ON CONFLICT (system_build_id) DO UPDATE
SET on_hand_quantity = EXCLUDED.on_hand_quantity,
    version = system_inventory_balances.version + 1,
    updated_at = CURRENT_TIMESTAMP;
