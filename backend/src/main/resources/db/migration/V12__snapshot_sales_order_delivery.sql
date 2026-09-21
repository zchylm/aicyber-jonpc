CREATE TABLE sales_order_delivery (
    sales_order_id UUID PRIMARY KEY REFERENCES sales_orders(id) ON DELETE CASCADE,
    recipient_name VARCHAR(160) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    address_line_1 VARCHAR(200) NOT NULL,
    address_line_2 VARCHAR(200),
    suburb VARCHAR(100) NOT NULL,
    state VARCHAR(20) NOT NULL,
    postcode VARCHAR(12) NOT NULL,
    country CHAR(2) NOT NULL DEFAULT 'AU',
    captured_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO sales_order_delivery
    (sales_order_id, recipient_name, phone, address_line_1, address_line_2, suburb, state, postcode, country)
SELECT s.id, d.recipient_name, d.phone, d.address_line_1, d.address_line_2,
       d.suburb, d.state, d.postcode, d.country
FROM sales_orders s
JOIN build_delivery_details d ON d.build_request_id = s.build_request_id;
