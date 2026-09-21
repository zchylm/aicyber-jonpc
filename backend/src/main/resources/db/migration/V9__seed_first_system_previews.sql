ALTER TABLE system_builds
    ADD COLUMN preview_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN listing_order INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN badge VARCHAR(40),
    ADD COLUMN planned_preorder_quantity INTEGER CHECK (planned_preorder_quantity IS NULL OR planned_preorder_quantity >= 0);

ALTER TABLE system_build_versions
    ADD COLUMN recommended_for VARCHAR(120),
    ADD COLUMN dispatch_estimate VARCHAR(120),
    ADD COLUMN specifications JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD CONSTRAINT system_build_versions_specifications_check CHECK (jsonb_typeof(specifications) = 'object');

INSERT INTO system_builds (id, code, name, direction, description, status, preview_enabled, listing_order, badge, planned_preorder_quantity) VALUES
    ('20000000-0000-0000-0000-000000000001', 'JON-STK-5060', 'JON PC Strike 5060', 'gaming', 'Built for smooth 1080p gaming.', 'DRAFT', TRUE, 1, NULL, 8),
    ('20000000-0000-0000-0000-000000000002', 'JON-STK-5060TI', 'JON PC Strike Ti 5060 Ti', 'gaming', 'A stronger starting point for 1440p gaming.', 'DRAFT', TRUE, 2, NULL, 8),
    ('20000000-0000-0000-0000-000000000003', 'JON-STM-5070', 'JON PC Storm 5070', 'gaming', 'High-refresh 1440p performance.', 'DRAFT', TRUE, 3, NULL, 2),
    ('20000000-0000-0000-0000-000000000004', 'JON-STM-5070TI', 'JON PC Storm Ti 5070 Ti', 'gaming', '1440p Ultra and entry 4K gaming.', 'DRAFT', TRUE, 4, 'Flagship', 2);

INSERT INTO system_build_versions (id, system_build_id, version, display_price_cents, currency, status, recommended_for, dispatch_estimate, specifications) VALUES
    ('21000000-0000-0000-0000-000000000001', '20000000-0000-0000-0000-000000000001', 1, 209900, 'AUD', 'DRAFT', '1080p High settings', '10–15 business days', '{"GPU":"NVIDIA GeForce RTX 5060 8GB GDDR7","CPU":"AMD Ryzen 5 7500F","Memory":"16GB DDR5-6000","Storage":"1TB Kingston NV3 NVMe PCIe 4.0 SSD","Cooler":"Thermalright AX120R SE ARGB","Motherboard":"GIGABYTE B850M EAGLE WIFI6E","PSU":"750W ATX 3.1","Case":"Thermaltake tempered glass ARGB mid-tower","Fans":"7 x 120mm ARGB fans + ARGB fan hub","Networking":"Wi-Fi 6E + Bluetooth 5.3 + 2.5G LAN","Accessories":"Logitech MK120 keyboard & mouse","OS":"Windows 11 Home","Warranty":"3-year parts / 2-year return-to-base"}'::jsonb),
    ('21000000-0000-0000-0000-000000000002', '20000000-0000-0000-0000-000000000002', 1, 239900, 'AUD', 'DRAFT', '1440p High settings', '10–15 business days', '{"GPU":"NVIDIA GeForce RTX 5060 Ti","CPU":"AMD Ryzen 5 7500F","Memory":"16GB DDR5-6000","Storage":"1TB Kingston NV3 NVMe PCIe 4.0 SSD","Cooler":"Thermalright AX120R SE ARGB","Motherboard":"GIGABYTE B850M EAGLE WIFI6E","PSU":"750W ATX 3.1","Case":"Thermaltake tempered glass ARGB mid-tower","Fans":"7 x 120mm ARGB fans + ARGB fan hub","Networking":"Wi-Fi 6E + Bluetooth 5.3 + 2.5G LAN","Accessories":"Logitech MK120 keyboard & mouse","OS":"Windows 11 Home","Warranty":"3-year parts / 2-year return-to-base"}'::jsonb),
    ('21000000-0000-0000-0000-000000000003', '20000000-0000-0000-0000-000000000003', 1, 289900, 'AUD', 'DRAFT', '1440p Ultra settings', '10–15 business days', '{"GPU":"NVIDIA GeForce RTX 5070 12GB GDDR7","CPU":"AMD Ryzen 5 7500F","Memory":"16GB DDR5-6000","Storage":"1TB Kingston NV3 NVMe PCIe 4.0 SSD","Cooler":"Thermalright AX120R SE ARGB","Motherboard":"GIGABYTE B850M EAGLE WIFI6E","PSU":"750W ATX 3.1","Case":"Thermaltake tempered glass ARGB mid-tower","Fans":"7 x 120mm ARGB fans + ARGB fan hub","Networking":"Wi-Fi 6E + Bluetooth 5.3 + 2.5G LAN","Accessories":"Logitech MK120 keyboard & mouse","OS":"Windows 11 Home","Warranty":"3-year parts / 2-year return-to-base"}'::jsonb),
    ('21000000-0000-0000-0000-000000000004', '20000000-0000-0000-0000-000000000004', 1, 349900, 'AUD', 'DRAFT', '1440p Ultra / entry 4K', '10–15 business days', '{"GPU":"NVIDIA GeForce RTX 5070 Ti 16GB GDDR7","CPU":"AMD Ryzen 5 7500F","Memory":"16GB DDR5-6000","Storage":"1TB Kingston NV3 NVMe PCIe 4.0 SSD","Cooler":"Thermalright AX120R SE ARGB","Motherboard":"GIGABYTE B850M EAGLE WIFI6E","PSU":"750W ATX 3.1","Case":"Thermaltake tempered glass ARGB mid-tower","Fans":"7 x 120mm ARGB fans + ARGB fan hub","Networking":"Wi-Fi 6E + Bluetooth 5.3 + 2.5G LAN","Accessories":"Logitech MK120 keyboard & mouse","OS":"Windows 11 Home","Warranty":"3-year parts / 2-year return-to-base"}'::jsonb);
