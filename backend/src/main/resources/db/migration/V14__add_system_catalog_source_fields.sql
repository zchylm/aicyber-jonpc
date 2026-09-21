ALTER TABLE system_builds
    ADD COLUMN product_range VARCHAR(40) NOT NULL DEFAULT 'Unassigned',
    ADD COLUMN sales_mode VARCHAR(40) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN source_revision VARCHAR(40) NOT NULL DEFAULT 'ADMIN',
    ADD CONSTRAINT system_builds_sales_mode_check
        CHECK (sales_mode IN ('STANDARD', 'PREORDER', 'STOCK_CHECK_REQUIRED'));

