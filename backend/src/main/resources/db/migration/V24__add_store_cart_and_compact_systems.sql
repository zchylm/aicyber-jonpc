ALTER TABLE system_builds
    ADD COLUMN sale_enabled BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE build_request_system_items (
    id UUID PRIMARY KEY,
    build_request_id UUID NOT NULL REFERENCES build_requests(id) ON DELETE CASCADE,
    system_build_id UUID NOT NULL REFERENCES system_builds(id),
    sku_snapshot VARCHAR(80) NOT NULL,
    name_snapshot VARCHAR(180) NOT NULL,
    unit_price_cents BIGINT NOT NULL CHECK (unit_price_cents > 0),
    quantity INTEGER NOT NULL CHECK (quantity > 0 AND quantity <= 10),
    line_total_cents BIGINT NOT NULL CHECK (line_total_cents > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (build_request_id, system_build_id)
);

CREATE INDEX build_request_system_items_request_idx
    ON build_request_system_items (build_request_id);

CREATE TABLE system_sale_reservations (
    sales_order_id UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    system_build_id UUID NOT NULL REFERENCES system_builds(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sales_order_id, system_build_id)
);

CREATE INDEX system_sale_reservations_build_expiry_idx
    ON system_sale_reservations (system_build_id, expires_at);

CREATE TABLE system_sale_allocation_items (
    sales_order_id UUID NOT NULL REFERENCES sales_orders(id) ON DELETE CASCADE,
    system_build_id UUID NOT NULL REFERENCES system_builds(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (sales_order_id, system_build_id)
);

CREATE INDEX system_sale_allocation_items_build_idx
    ON system_sale_allocation_items (system_build_id);

INSERT INTO system_builds
    (id, code, name, direction, description, status, preview_enabled, sale_enabled,
     listing_order, badge, planned_preorder_quantity, product_range, sales_mode, source_revision)
VALUES
    ('20000000-0000-0000-0000-000000000021', 'JON-EDGE-MINI', 'Edge Mini', 'ai',
     'A quiet, compact starting point for local AI tools, automation and everyday development.',
     'DRAFT', TRUE, FALSE, 21, NULL, 0, 'Compact AI', 'STANDARD', '2026-10-21'),
    ('20000000-0000-0000-0000-000000000022', 'JON-EDGE-AI', 'Edge AI', 'ai',
     'A balanced AI PC for development, content tools and accelerated local workflows.',
     'DRAFT', TRUE, FALSE, 22, NULL, 0, 'Compact AI', 'STANDARD', '2026-10-21'),
    ('20000000-0000-0000-0000-000000000023', 'JON-EDGE-PRO', 'Edge Pro', 'ai',
     'More memory and storage for heavier local models, data preparation and professional multitasking.',
     'DRAFT', TRUE, FALSE, 23, NULL, 0, 'Compact AI', 'STANDARD', '2026-10-21'),
    ('20000000-0000-0000-0000-000000000024', 'JON-EDGE-MAX', 'Edge Max', 'ai',
     'A compact local-AI workstation with unified memory for substantially larger models and datasets.',
     'DRAFT', TRUE, FALSE, 24, NULL, 0, 'Compact AI', 'STANDARD', '2026-10-21');

INSERT INTO system_inventory_balances (system_build_id, on_hand_quantity)
SELECT id, 0 FROM system_builds WHERE code LIKE 'JON-EDGE-%';

INSERT INTO system_build_versions
    (id, system_build_id, version, display_price_cents, currency, status,
     recommended_for, dispatch_estimate, specifications)
VALUES
    ('21000000-0000-0000-0000-000000000021', '20000000-0000-0000-0000-000000000021', 1, 129900, 'AUD', 'DRAFT',
     'Local assistants · automation · office AI', NULL,
     jsonb_build_object('Processor','AMD Ryzen 7 8845HS · 8 cores / 16 threads','Graphics','Integrated Radeon 780M','AI acceleration','Ryzen AI NPU · up to 16 TOPS','Memory','32GB DDR5','Storage','1TB PCIe 4.0 NVMe','Networking','2.5GbE · Wi-Fi 6 · Bluetooth','Display','USB-C · HDMI · DisplayPort','OS','Windows 11 Pro or Linux deployment')),
    ('21000000-0000-0000-0000-000000000022', '20000000-0000-0000-0000-000000000022', 1, 189900, 'AUD', 'DRAFT',
     'AI development · creator tools · edge inference', NULL,
     jsonb_build_object('Processor','AMD Ryzen AI 9 HX 370 · 12 cores / 24 threads','Graphics','Integrated Radeon 890M','AI acceleration','XDNA 2 NPU · up to 50 TOPS','Memory','32GB DDR5','Storage','1TB PCIe 4.0 NVMe','Networking','Dual 2.5GbE · Wi-Fi 7 · Bluetooth','Expansion','USB4 · multiple NVMe support','OS','Windows 11 Pro or Linux deployment')),
    ('21000000-0000-0000-0000-000000000023', '20000000-0000-0000-0000-000000000023', 1, 269900, 'AUD', 'DRAFT',
     'Local models · data workflows · AI production', NULL,
     jsonb_build_object('Processor','AMD Ryzen AI 9 HX 370 · 12 cores / 24 threads','Graphics','Integrated Radeon 890M','AI acceleration','XDNA 2 NPU · up to 50 TOPS','Memory','64GB DDR5','Storage','2TB PCIe 4.0 NVMe','Networking','Dual 2.5GbE · Wi-Fi 7 · Bluetooth','Expansion','USB4 · multi-display · additional NVMe','OS','Windows 11 Pro or Linux deployment')),
    ('21000000-0000-0000-0000-000000000024', '20000000-0000-0000-0000-000000000024', 1, 499900, 'AUD', 'DRAFT',
     'Large local models · AI research · compact workstation', NULL,
     jsonb_build_object('Processor','AMD Ryzen AI Max+ 395 · 16 cores / 32 threads','Graphics','Integrated Radeon 8060S · 40 compute units','AI acceleration','Up to 126 combined platform TOPS','Memory','128GB LPDDR5X unified memory','Storage','2TB PCIe 4.0 NVMe','Networking','Dual 2.5GbE · Wi-Fi 7 · Bluetooth','Expansion','USB4 · multi-display · additional NVMe','OS','Windows 11 Pro or Linux deployment'));
