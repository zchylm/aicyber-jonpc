-- Retire only the eight untouched, unused drafts imported by V15.
CREATE TEMP TABLE jonpc_retired_system_drafts (id UUID PRIMARY KEY, code TEXT NOT NULL) ON COMMIT DROP;
INSERT INTO jonpc_retired_system_drafts VALUES
    ('20000000-0000-0000-0000-000000000005', 'S1'),
    ('20000000-0000-0000-0000-000000000006', 'A1'),
    ('20000000-0000-0000-0000-000000000007', 'A2'),
    ('20000000-0000-0000-0000-000000000008', 'A3'),
    ('20000000-0000-0000-0000-000000000009', 'A4'),
    ('20000000-0000-0000-0000-000000000010', 'B3'),
    ('20000000-0000-0000-0000-000000000011', 'C1'),
    ('20000000-0000-0000-0000-000000000012', 'C2');

LOCK TABLE system_builds, system_build_versions, system_inventory_balances IN SHARE ROW EXCLUSIVE MODE;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM system_builds s JOIN jonpc_retired_system_drafts d ON d.id = s.id
        WHERE s.code <> d.code OR s.status <> 'DRAFT' OR s.preview_enabled OR s.sale_enabled
           OR s.source_revision <> '2026-09-16' OR s.planned_preorder_quantity <> 0
           OR EXISTS (SELECT 1 FROM system_inventory_balances b WHERE b.system_build_id = s.id AND b.on_hand_quantity <> 0)
           OR EXISTS (SELECT 1 FROM system_build_versions v WHERE v.system_build_id = s.id AND (v.version <> 1 OR v.status <> 'DRAFT'))
           OR EXISTS (SELECT 1 FROM sales_orders o JOIN system_build_versions v ON v.id = o.source_build_version_id WHERE v.system_build_id = s.id)
           OR EXISTS (SELECT 1 FROM build_request_system_items i WHERE i.system_build_id = s.id)
           OR EXISTS (SELECT 1 FROM system_sale_allocations a WHERE a.system_build_id = s.id)
           OR EXISTS (SELECT 1 FROM system_sale_allocation_items a WHERE a.system_build_id = s.id)
           OR EXISTS (SELECT 1 FROM system_sale_reservations r WHERE r.system_build_id = s.id)
           OR EXISTS (SELECT 1 FROM system_inventory_movements m WHERE m.system_build_id = s.id)
           OR EXISTS (SELECT 1 FROM build_requests r WHERE r.configuration_snapshot->'answers'->>'systemId' = s.id::text
                       OR r.configuration_snapshot->'answers'->>'systemSku' = s.code)
    ) THEN
        RAISE EXCEPTION 'Legacy system draft is modified, enabled, stocked or referenced; review before removal';
    END IF;
END $$;

DELETE FROM system_build_components c USING system_build_versions v, jonpc_retired_system_drafts d
WHERE c.build_version_id = v.id AND v.system_build_id = d.id;
DELETE FROM system_inventory_balances b USING jonpc_retired_system_drafts d WHERE b.system_build_id = d.id;
DELETE FROM system_build_versions v USING jonpc_retired_system_drafts d WHERE v.system_build_id = d.id;
DELETE FROM system_builds s USING jonpc_retired_system_drafts d WHERE s.id = d.id;
