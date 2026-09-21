UPDATE system_builds
SET product_range = 'Strike/Storm',
    sales_mode = 'PREORDER',
    source_revision = '2026-09-16',
    listing_order = CASE code
        WHEN 'JON-STK-5060' THEN 9
        WHEN 'JON-STK-5060TI' THEN 10
        WHEN 'JON-STM-5070' THEN 11
        WHEN 'JON-STM-5070TI' THEN 12
    END,
    updated_at = CURRENT_TIMESTAMP
WHERE code IN ('JON-STK-5060', 'JON-STK-5060TI', 'JON-STM-5070', 'JON-STM-5070TI');

INSERT INTO system_builds
    (id, code, name, direction, description, status, preview_enabled, listing_order, badge,
     planned_preorder_quantity, product_range, sales_mode, source_revision)
VALUES
    ('20000000-0000-0000-0000-000000000005', 'S1', 'JON PC S1 Flagship – RTX 5090', 'performance', NULL, 'DRAFT', FALSE, 1, NULL, 0, 'Premium', 'STOCK_CHECK_REQUIRED', '2026-09-16'),
    ('20000000-0000-0000-0000-000000000006', 'A1', 'JON PC A1 High-End – RTX 5080 White', 'performance', NULL, 'DRAFT', FALSE, 2, NULL, 0, 'Premium', 'STOCK_CHECK_REQUIRED', '2026-09-16'),
    ('20000000-0000-0000-0000-000000000007', 'A2', 'JON PC A2 High-End – RTX 5080', 'performance', NULL, 'DRAFT', FALSE, 3, NULL, 0, 'Premium', 'STOCK_CHECK_REQUIRED', '2026-09-16'),
    ('20000000-0000-0000-0000-000000000008', 'A3', 'JON PC A3 High-End – RTX 5070 Ti', 'performance', NULL, 'DRAFT', FALSE, 4, NULL, 0, 'Premium', 'STOCK_CHECK_REQUIRED', '2026-09-16'),
    ('20000000-0000-0000-0000-000000000009', 'A4', 'JON PC A4 High-End – RTX 5070 Ti', 'performance', NULL, 'DRAFT', FALSE, 5, NULL, 0, 'Premium', 'STOCK_CHECK_REQUIRED', '2026-09-16'),
    ('20000000-0000-0000-0000-000000000010', 'B3', 'JON PC B3 Performance – RTX 5070', 'performance', NULL, 'DRAFT', FALSE, 6, NULL, 0, 'Premium', 'STOCK_CHECK_REQUIRED', '2026-09-16'),
    ('20000000-0000-0000-0000-000000000011', 'C1', 'JON PC C1 Mainstream – RTX 5060 Ti 2TB', 'performance', NULL, 'DRAFT', FALSE, 7, NULL, 0, 'Premium', 'STOCK_CHECK_REQUIRED', '2026-09-16'),
    ('20000000-0000-0000-0000-000000000012', 'C2', 'JON PC C2 Mainstream – RTX 5060 Ti 1TB', 'performance', NULL, 'DRAFT', FALSE, 8, NULL, 0, 'Premium', 'STOCK_CHECK_REQUIRED', '2026-09-16');

INSERT INTO system_inventory_balances (system_build_id, on_hand_quantity)
SELECT id, 0
FROM system_builds
WHERE code IN ('S1', 'A1', 'A2', 'A3', 'A4', 'B3', 'C1', 'C2');

INSERT INTO system_build_versions
    (id, system_build_id, version, display_price_cents, currency, status, recommended_for,
     dispatch_estimate, specifications)
VALUES
    ('21000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000005', 1, 2319900, 'AUD', 'DRAFT',
     'Ultimate 4K gaming, AI workloads & content creation', NULL,
     jsonb_build_object('CPU','AMD Ryzen 9 9950X3D','GPU','ASUS ROG GeForce RTX 5090 32GB White','Memory','128GB DDR5-6000','Storage','4TB WD SN8100 PCIe 5.0 NVMe','Motherboard','ASUS ROG STRIX X870-A WIFI','Cooler','Thermaltake ST360 Ultra 360mm ARGB LCD AIO (White)','PSU','Great Wall F12 Platinum ATX 3.1 (White)','Case','Thermaltake panoramic display case (White)','Accessories','Aigo DK108 mechanical keyboard & mouse (White)','OS','Windows 11 (pre-installed)','Networking','Wi-Fi 6E + Bluetooth (onboard)','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000006', '20000000-0000-0000-0000-000000000006', 1, 1189900, 'AUD', 'DRAFT',
     'Premium 4K gaming & professional creation', NULL,
     jsonb_build_object('CPU','AMD Ryzen 9 9950X3D','GPU','ASUS ROG GeForce RTX 5080 16GB White','Memory','64GB DDR5-6000','Storage','4TB WD SN8100 PCIe 5.0 NVMe','Motherboard','ASUS ROG STRIX X870-A WIFI','Cooler','Thermaltake ST360 Ultra 360mm ARGB LCD AIO (White)','PSU','Thermaltake 1000W 80+ Gold ATX 3.1 (White)','Case','Thermaltake panoramic display case (White)','Accessories','Aigo DK108 mechanical keyboard & mouse (White)','OS','Windows 11 (pre-installed)','Networking','Wi-Fi 6E + Bluetooth (onboard)','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000007', '20000000-0000-0000-0000-000000000007', 1, 829900, 'AUD', 'DRAFT',
     '4K gaming, streaming & heavy multitasking', NULL,
     jsonb_build_object('CPU','AMD Ryzen 9 9950X','GPU','MSI GeForce RTX 5080 16GB VENTUS 3X OC','Memory','32GB DDR5-6000','Storage','4TB WD SN8100 PCIe 5.0 NVMe','Motherboard','MSI MAG X870 TOMAHAWK WIFI','Cooler','Thermaltake ST360 Ultra 360mm ARGB LCD AIO (Black)','PSU','Thermaltake 1000W 80+ Gold ATX 3.1','Case','Thermaltake panoramic display case (Black)','Accessories','Aigo DK108 mechanical keyboard & mouse (Black)','OS','Windows 11 (pre-installed)','Networking','Wi-Fi 6E + Bluetooth (onboard)','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000008', '20000000-0000-0000-0000-000000000008', 1, 649900, 'AUD', 'DRAFT',
     'Competitive QHD gaming & high-FPS play', NULL,
     jsonb_build_object('CPU','AMD Ryzen 7 9850X3D','GPU','MSI GeForce RTX 5070 Ti 16GB VENTUS 3X OC','Memory','32GB DDR5-6000','Storage','2TB Samsung 9100 PRO NVMe','Motherboard','MSI MAG X870 TOMAHAWK WIFI','Cooler','Thermaltake ST360 Ultra 360mm ARGB LCD AIO (Black)','PSU','Thermaltake 1000W 80+ Gold ATX 3.1','Case','Thermaltake panoramic display case (Black)','Accessories','Aigo DK108 mechanical keyboard & mouse (Black)','OS','Windows 11 (pre-installed)','Networking','Wi-Fi 6E + Bluetooth (onboard)','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000009', '20000000-0000-0000-0000-000000000009', 1, 614900, 'AUD', 'DRAFT',
     'High-FPS QHD gaming & esports', NULL,
     jsonb_build_object('CPU','AMD Ryzen 7 9800X3D','GPU','MSI GeForce RTX 5070 Ti 16GB VENTUS 3X OC','Memory','32GB DDR5-6000','Storage','2TB Samsung 9100 PRO NVMe','Motherboard','ASUS TUF GAMING B850M-E WIFI','Cooler','Thermaltake 360mm ARGB 2.4-inch LCD AIO (Black)','PSU','Thermaltake 1000W 80+ Gold ATX 3.1','Case','Thermaltake panoramic display case (Black)','Accessories','Aigo DK108 mechanical keyboard & mouse (Black)','OS','Windows 11 (pre-installed)','Networking','Wi-Fi 6E + Bluetooth (onboard)','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000010', '20000000-0000-0000-0000-000000000010', 1, 504900, 'AUD', 'DRAFT',
     'QHD gaming, productivity & content creation', NULL,
     jsonb_build_object('CPU','AMD Ryzen 9 9900X','GPU','MSI GeForce RTX 5070 12GB VENTUS 2X OC','Memory','32GB DDR5-6000','Storage','2TB KIOXIA SF10 PCIe 4.0 NVMe','Motherboard','MSI PRO B850M-A WIFI','Cooler','ID-COOLING DF360 ARGB 360mm AIO','PSU','Thermaltake 850W 80+ Gold ATX 3.1','Case','Thermaltake panoramic display case (Black)','Accessories','Aigo DK108 mechanical keyboard & mouse (Black)','OS','Windows 11 (pre-installed)','Networking','Wi-Fi 6E + Bluetooth (onboard)','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000011', '20000000-0000-0000-0000-000000000011', 1, 369900, 'AUD', 'DRAFT',
     'QHD gaming with extra storage', NULL,
     jsonb_build_object('CPU','AMD Ryzen 7 9700X','GPU','ZOTAC GeForce RTX 5060 Ti 8GB','Memory','32GB DDR5-6000','Storage','2TB KIOXIA SF10 PCIe 4.0 NVMe','Motherboard','MSI PRO B850M-A WIFI','Cooler','ID-COOLING SE-55 ARGB dual-tower air cooler','PSU','Thermaltake 850W 80+ Gold ATX 3.1','Case','Thermaltake panoramic display case (Black)','Accessories','Aigo DK108 mechanical keyboard & mouse (Black)','OS','Windows 11 (pre-installed)','Networking','Wi-Fi 6E + Bluetooth (onboard)','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000012', '20000000-0000-0000-0000-000000000012', 1, 344900, 'AUD', 'DRAFT',
     'Mainstream QHD gaming', NULL,
     jsonb_build_object('CPU','AMD Ryzen 7 9700X','GPU','ZOTAC GeForce RTX 5060 Ti 8GB','Memory','32GB DDR5-6000','Storage','1TB KIOXIA SF10 PCIe 4.0 NVMe','Motherboard','MSI PRO B850M-A WIFI','Cooler','ID-COOLING SE-55 ARGB dual-tower air cooler','PSU','Thermaltake 850W 80+ Gold ATX 3.1','Case','Thermaltake panoramic display case (Black)','Accessories','Logitech MK120 keyboard & mouse','OS','Windows 11 (pre-installed)','Networking','Wi-Fi 6E + Bluetooth (onboard)','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000013', '20000000-0000-0000-0000-000000000001', 2, 254900, 'AUD', 'DRAFT',
     '1080p High settings', '10–15 business days',
     jsonb_build_object('CPU','AMD Ryzen 5 7500F (6C/12T, up to 5.0GHz)','GPU','NVIDIA GeForce RTX 5060 8GB GDDR7','Memory','16GB DDR5-6000','Storage','1TB Kingston NV3 NVMe PCIe 4.0','Motherboard','GIGABYTE B850M EAGLE WIFI6E','Cooler','Thermalright AX120R SE ARGB','PSU','750W ATX 3.1 (12V-2x6)','Case','Thermaltake tempered glass ARGB mid-tower (7x120mm ARGB fans + hub)','Accessories','Logitech MK120 keyboard & mouse','OS','Windows 11 Home (activated)','Networking','Wi-Fi 6E + Bluetooth 5.3 + 2.5G LAN','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000014', '20000000-0000-0000-0000-000000000002', 2, 279900, 'AUD', 'DRAFT',
     '1440p High settings', '10–15 business days',
     jsonb_build_object('CPU','AMD Ryzen 5 7500F (6C/12T, up to 5.0GHz)','GPU','NVIDIA GeForce RTX 5060 Ti 16GB GDDR7','Memory','16GB DDR5-6000','Storage','1TB Kingston NV3 NVMe PCIe 4.0','Motherboard','GIGABYTE B850M EAGLE WIFI6E','Cooler','Thermalright AX120R SE ARGB','PSU','750W ATX 3.1 (12V-2x6)','Case','Thermaltake tempered glass ARGB mid-tower (7x120mm ARGB fans + hub)','Accessories','Logitech MK120 keyboard & mouse','OS','Windows 11 Home (activated)','Networking','Wi-Fi 6E + Bluetooth 5.3 + 2.5G LAN','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000015', '20000000-0000-0000-0000-000000000003', 2, 329900, 'AUD', 'DRAFT',
     '1440p Ultra settings', '10–15 business days',
     jsonb_build_object('CPU','AMD Ryzen 5 7500F (6C/12T, up to 5.0GHz)','GPU','NVIDIA GeForce RTX 5070 12GB GDDR7','Memory','16GB DDR5-6000','Storage','1TB Kingston NV3 NVMe PCIe 4.0','Motherboard','GIGABYTE B850M EAGLE WIFI6E','Cooler','Thermalright AX120R SE ARGB','PSU','750W ATX 3.1 (12V-2x6)','Case','Thermaltake tempered glass ARGB mid-tower (7x120mm ARGB fans + hub)','Accessories','Logitech MK120 keyboard & mouse','OS','Windows 11 Home (activated)','Networking','Wi-Fi 6E + Bluetooth 5.3 + 2.5G LAN','Warranty','3-year parts / 2-year return-to-base')),
    ('21000000-0000-0000-0000-000000000016', '20000000-0000-0000-0000-000000000004', 2, 394900, 'AUD', 'DRAFT',
     '1440p Ultra / entry 4K', '10–15 business days',
     jsonb_build_object('CPU','AMD Ryzen 5 7500F (6C/12T, up to 5.0GHz)','GPU','NVIDIA GeForce RTX 5070 Ti 16GB GDDR7','Memory','16GB DDR5-6000','Storage','1TB Kingston NV3 NVMe PCIe 4.0','Motherboard','GIGABYTE B850M EAGLE WIFI6E','Cooler','Thermalright AX120R SE ARGB','PSU','750W ATX 3.1 (12V-2x6)','Case','Thermaltake tempered glass ARGB mid-tower (7x120mm ARGB fans + hub)','Accessories','Logitech MK120 keyboard & mouse','OS','Windows 11 Home (activated)','Networking','Wi-Fi 6E + Bluetooth 5.3 + 2.5G LAN','Warranty','3-year parts / 2-year return-to-base'));
