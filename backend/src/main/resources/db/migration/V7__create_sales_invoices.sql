CREATE TABLE invoice_number_sequences (
    invoice_year INTEGER PRIMARY KEY CHECK (invoice_year BETWEEN 2000 AND 9999),
    last_number BIGINT NOT NULL CHECK (last_number > 0)
);

CREATE TABLE sales_invoices (
    id UUID PRIMARY KEY,
    sales_order_id UUID NOT NULL UNIQUE REFERENCES sales_orders(id),
    payment_id UUID NOT NULL UNIQUE REFERENCES payments(id),
    invoice_number VARCHAR(40) NOT NULL UNIQUE,
    document_type VARCHAR(30) NOT NULL DEFAULT 'TAX_INVOICE',
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    order_reference VARCHAR(40) NOT NULL,
    payment_reference VARCHAR(40) NOT NULL,
    seller_legal_name VARCHAR(200) NOT NULL,
    seller_trading_name VARCHAR(160) NOT NULL,
    seller_abn VARCHAR(20) NOT NULL,
    seller_address_line VARCHAR(240) NOT NULL,
    seller_email VARCHAR(320) NOT NULL,
    seller_phone VARCHAR(40) NOT NULL,
    buyer_name VARCHAR(200) NOT NULL,
    buyer_email VARCHAR(320) NOT NULL,
    buyer_location VARCHAR(200),
    buyer_abn VARCHAR(20),
    currency CHAR(3) NOT NULL DEFAULT 'AUD',
    subtotal_ex_gst_cents BIGINT NOT NULL CHECK (subtotal_ex_gst_cents >= 0),
    gst_cents BIGINT NOT NULL CHECK (gst_cents >= 0),
    total_cents BIGINT NOT NULL CHECK (total_cents > 0),
    amount_paid_cents BIGINT NOT NULL CHECK (amount_paid_cents > 0),
    issued_at TIMESTAMPTZ NOT NULL,
    pdf_content BYTEA NOT NULL,
    pdf_sha256 CHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sales_invoices_document_type_check CHECK (
        document_type IN ('TAX_INVOICE', 'INVOICE', 'ADJUSTMENT_NOTE')
    ),
    CONSTRAINT sales_invoices_status_check CHECK (status IN ('ISSUED', 'VOID')),
    CONSTRAINT sales_invoices_currency_check CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT sales_invoices_amounts_check CHECK (
        subtotal_ex_gst_cents + gst_cents = total_cents
        AND amount_paid_cents = total_cents
    )
);

CREATE INDEX sales_invoices_issued_at_idx ON sales_invoices (issued_at DESC);
CREATE INDEX sales_invoices_buyer_email_idx ON sales_invoices (LOWER(buyer_email));

CREATE TABLE sales_invoice_lines (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES sales_invoices(id),
    line_number INTEGER NOT NULL CHECK (line_number > 0),
    sku_snapshot VARCHAR(80),
    description_snapshot VARCHAR(300) NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price_ex_gst_cents BIGINT NOT NULL CHECK (unit_price_ex_gst_cents >= 0),
    gst_rate_basis_points INTEGER NOT NULL CHECK (gst_rate_basis_points BETWEEN 0 AND 10000),
    gst_cents BIGINT NOT NULL CHECK (gst_cents >= 0),
    line_total_cents BIGINT NOT NULL CHECK (line_total_cents > 0),
    taxable BOOLEAN NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT sales_invoice_lines_number_unique UNIQUE (invoice_id, line_number),
    CONSTRAINT sales_invoice_lines_amounts_check CHECK (
        unit_price_ex_gst_cents * quantity + gst_cents = line_total_cents
    )
);

CREATE TABLE invoice_delivery_attempts (
    id UUID PRIMARY KEY,
    invoice_id UUID NOT NULL REFERENCES sales_invoices(id),
    recipient_email VARCHAR(320) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    provider_message_id VARCHAR(200),
    last_error VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_attempted_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    CONSTRAINT invoice_delivery_attempts_status_check CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    CONSTRAINT invoice_delivery_attempts_sent_check CHECK (
        status <> 'SENT' OR sent_at IS NOT NULL
    )
);

CREATE INDEX invoice_delivery_attempts_pending_idx
    ON invoice_delivery_attempts (created_at)
    WHERE status IN ('PENDING', 'FAILED');
