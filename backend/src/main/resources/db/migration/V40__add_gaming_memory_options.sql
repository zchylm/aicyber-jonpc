CREATE TEMP TABLE gaming_memory_prices (
    code TEXT PRIMARY KEY,
    price_16 INTEGER NOT NULL,
    price_32 INTEGER NOT NULL,
    system_32_id UUID NOT NULL
) ON COMMIT DROP;

INSERT INTO gaming_memory_prices VALUES
    ('JON-STK-5060',   209900, 229900, '20000000-0000-0000-0000-000000000041'),
    ('JON-STK-5060TI', 249900, 269900, '20000000-0000-0000-0000-000000000042'),
    ('JON-STM-5070',   269900, 289900, '20000000-0000-0000-0000-000000000043'),
    ('JON-STM-5070TI', 349900, 369900, '20000000-0000-0000-0000-000000000044');

INSERT INTO system_builds
    (id, code, name, direction, description, status, preview_enabled, sale_enabled, listing_order, badge,
     planned_preorder_quantity, product_range, sales_mode, source_revision)
SELECT p.system_32_id, s.code || '-32GB', s.name || ' · 32GB RAM', s.direction, s.description, 'DRAFT',
       s.preview_enabled, FALSE, s.listing_order, s.badge, 0, s.product_range, s.sales_mode, s.source_revision
FROM system_builds s JOIN gaming_memory_prices p ON p.code = s.code;

-- A separately assembled variant gets its own stock receipt before checkout is enabled.
INSERT INTO system_inventory_balances (system_build_id, on_hand_quantity)
SELECT system_32_id, 0 FROM gaming_memory_prices;

INSERT INTO system_build_versions
    (id, system_build_id, version, display_price_cents, currency, status, recommended_for, dispatch_estimate, specifications)
SELECT gen_random_uuid(), o.system_id, o.version, o.price, v.currency, 'DRAFT', v.recommended_for,
       v.dispatch_estimate, o.specifications
FROM system_builds s
JOIN gaming_memory_prices p ON p.code = s.code
JOIN LATERAL (
    SELECT * FROM system_build_versions WHERE system_build_id = s.id ORDER BY version DESC LIMIT 1
) v ON TRUE
CROSS JOIN LATERAL (
    VALUES (s.id, v.version + 1, p.price_16, v.specifications),
           (p.system_32_id, 1, p.price_32, jsonb_set(v.specifications, '{Memory}', '"32GB DDR5 (2 × 16GB)"'))
) o(system_id, version, price, specifications);

UPDATE system_builds s
SET name = s.name || ' · 16GB RAM', updated_at = CURRENT_TIMESTAMP
FROM gaming_memory_prices p
WHERE p.code = s.code;
