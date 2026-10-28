ALTER TABLE build_delivery_details
    ADD COLUMN delivery_method VARCHAR(20) NOT NULL DEFAULT 'DELIVERY',
    ADD CONSTRAINT build_delivery_details_method_check CHECK (delivery_method IN ('DELIVERY', 'PICKUP'));

ALTER TABLE sales_order_delivery
    ADD COLUMN delivery_method VARCHAR(20) NOT NULL DEFAULT 'DELIVERY',
    ADD CONSTRAINT sales_order_delivery_method_check CHECK (delivery_method IN ('DELIVERY', 'PICKUP'));
