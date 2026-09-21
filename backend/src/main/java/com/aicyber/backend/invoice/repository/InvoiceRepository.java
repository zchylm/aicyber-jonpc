package com.aicyber.backend.invoice.repository;

import com.aicyber.backend.invoice.model.InvoiceDeliveryAttempt;
import com.aicyber.backend.invoice.model.InvoiceLine;
import com.aicyber.backend.invoice.model.InvoiceSource;
import com.aicyber.backend.invoice.model.SalesInvoice;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InvoiceRepository {
    private final JdbcTemplate jdbcTemplate;

    public InvoiceRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<InvoiceSource> sourceForIssue(UUID orderId, UUID paymentId) {
        return jdbcTemplate.query("""
                SELECT s.id AS order_id, p.id AS payment_id, s.user_id, s.order_reference,
                       p.payment_reference, s.amount_cents AS order_amount_cents,
                       p.amount_cents AS payment_amount_cents, s.currency AS order_currency,
                       p.currency AS payment_currency, s.status AS order_status, p.status AS payment_status,
                       COALESCE(NULLIF(b.name, ''), u.display_name) AS buyer_name,
                       COALESCE(NULLIF(b.email, ''), u.email) AS buyer_email, u.customer_reference,
                       COALESCE(d.suburb || ' ' || d.state || ' ' || d.postcode, b.location) AS buyer_location,
                       b.direction,
                       b.configuration_snapshot->'answers'->>'systemSku' AS system_sku,
                       b.configuration_snapshot->'answers'->>'systemName' AS system_name,
                       c.founder_sequence, t.display_name AS founder_tier_name,
                       c.cashback_amount_cents AS founder_cashback_amount_cents
                FROM sales_orders s
                JOIN payments p ON p.order_id = s.id
                JOIN users u ON u.id = s.user_id
                LEFT JOIN build_requests b ON b.id = s.build_request_id
                LEFT JOIN build_delivery_details d ON d.build_request_id = b.id
                LEFT JOIN reward_commitments c ON c.order_id = s.id AND c.status <> 'VOID'
                LEFT JOIN reward_founder_tiers t ON t.id = c.tier_id
                WHERE s.id = ? AND p.id = ?
                """, (resultSet, rowNum) -> new InvoiceSource(
                resultSet.getObject("order_id", UUID.class),
                resultSet.getObject("payment_id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("order_reference"),
                resultSet.getString("payment_reference"),
                resultSet.getLong("order_amount_cents"),
                resultSet.getLong("payment_amount_cents"),
                resultSet.getString("order_currency").trim(),
                resultSet.getString("payment_currency").trim(),
                resultSet.getString("order_status"),
                resultSet.getString("payment_status"),
                resultSet.getString("buyer_name"),
                resultSet.getString("buyer_email"),
                resultSet.getString("customer_reference"),
                resultSet.getString("buyer_location"),
                resultSet.getString("direction"),
                resultSet.getString("system_sku"),
                resultSet.getString("system_name"),
                resultSet.getObject("founder_sequence", Integer.class),
                resultSet.getString("founder_tier_name"),
                resultSet.getObject("founder_cashback_amount_cents", Long.class)
        ), orderId, paymentId).stream().findFirst();
    }

    public String nextInvoiceNumber(int year) {
        Long number = jdbcTemplate.queryForObject("""
                INSERT INTO invoice_number_sequences (invoice_year, last_number)
                VALUES (?, 1)
                ON CONFLICT (invoice_year)
                DO UPDATE SET last_number = invoice_number_sequences.last_number + 1
                RETURNING last_number
                """, Long.class, year);
        if (number == null) throw new IllegalStateException("Invoice number could not be allocated");
        return "JON-INV-%d-%06d".formatted(year, number);
    }

    public void create(SalesInvoice invoice) {
        jdbcTemplate.update("""
                INSERT INTO sales_invoices
                    (id, sales_order_id, payment_id, invoice_number, document_type, status,
                     order_reference, payment_reference, seller_legal_name, seller_trading_name,
                     seller_abn, seller_address_line, seller_email, seller_phone, buyer_name,
                     buyer_email, buyer_customer_reference, buyer_location, buyer_abn, currency, subtotal_ex_gst_cents,
                     gst_cents, total_cents, amount_paid_cents, issued_at, founder_number,
                     founder_tier_name, founder_cashback_amount_cents, founder_cashback_terms,
                     pdf_content, pdf_sha256)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                invoice.id(), invoice.orderId(), invoice.paymentId(), invoice.invoiceNumber(),
                invoice.documentType(), invoice.status(), invoice.orderReference(), invoice.paymentReference(),
                invoice.sellerLegalName(), invoice.sellerTradingName(), invoice.sellerAbn(),
                invoice.sellerAddress(), invoice.sellerEmail(), invoice.sellerPhone(), invoice.buyerName(),
                invoice.buyerEmail(), invoice.buyerCustomerReference(), invoice.buyerLocation(), invoice.buyerAbn(), invoice.currency(),
                invoice.subtotalExGstCents(), invoice.gstCents(), invoice.totalCents(),
                invoice.amountPaidCents(), invoice.issuedAt(), invoice.founderNumber(), invoice.founderTierName(),
                invoice.founderCashbackAmountCents(), invoice.founderCashbackTerms(), invoice.pdfContent(), invoice.pdfSha256()
        );
        for (InvoiceLine line : invoice.lines()) {
            jdbcTemplate.update("""
                    INSERT INTO sales_invoice_lines
                        (id, invoice_id, line_number, sku_snapshot, description_snapshot, quantity,
                         unit_price_ex_gst_cents, gst_rate_basis_points, gst_cents, line_total_cents, taxable)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """, line.id(), invoice.id(), line.lineNumber(), line.sku(), line.description(),
                    line.quantity(), line.unitPriceExGstCents(), line.gstRateBasisPoints(), line.gstCents(),
                    line.lineTotalCents(), line.taxable());
        }
        jdbcTemplate.update("""
                INSERT INTO invoice_delivery_attempts (id, invoice_id, recipient_email)
                VALUES (?, ?, ?)
                """, UUID.randomUUID(), invoice.id(), invoice.buyerEmail());
    }

    public Optional<SalesInvoice> findByOrder(UUID orderId) {
        return find("WHERE i.sales_order_id = ?", orderId);
    }

    public Optional<SalesInvoice> findOwned(UUID invoiceId, UUID userId) {
        return find("WHERE i.id = ? AND s.user_id = ?", invoiceId, userId);
    }

    public Optional<SalesInvoice> findById(UUID invoiceId) {
        return find("WHERE i.id = ?", invoiceId);
    }

    public Optional<InvoiceDeliveryAttempt> deliveryAttempt(UUID invoiceId) {
        return jdbcTemplate.query("""
                SELECT id, invoice_id, recipient_email, status, attempt_count
                FROM invoice_delivery_attempts WHERE invoice_id = ?
                ORDER BY created_at LIMIT 1
                """, (resultSet, rowNum) -> new InvoiceDeliveryAttempt(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("invoice_id", UUID.class),
                resultSet.getString("recipient_email"),
                resultSet.getString("status"),
                resultSet.getInt("attempt_count")
        ), invoiceId).stream().findFirst();
    }

    public Optional<InvoiceDeliveryAttempt> latestDeliveryAttempt(UUID invoiceId) {
        return jdbcTemplate.query("""
                SELECT id, invoice_id, recipient_email, status, attempt_count
                FROM invoice_delivery_attempts WHERE invoice_id = ?
                ORDER BY created_at DESC, id DESC LIMIT 1
                """, (resultSet, rowNum) -> new InvoiceDeliveryAttempt(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("invoice_id", UUID.class),
                resultSet.getString("recipient_email"),
                resultSet.getString("status"),
                resultSet.getInt("attempt_count")
        ), invoiceId).stream().findFirst();
    }

    public InvoiceDeliveryAttempt createDeliveryAttempt(UUID invoiceId, String recipientEmail) {
        UUID attemptId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO invoice_delivery_attempts (id, invoice_id, recipient_email)
                VALUES (?, ?, ?)
                """, attemptId, invoiceId, recipientEmail);
        return new InvoiceDeliveryAttempt(attemptId, invoiceId, recipientEmail, "PENDING", 0);
    }

    public void markDeliverySent(UUID attemptId, String providerMessageId) {
        jdbcTemplate.update("""
                UPDATE invoice_delivery_attempts
                SET status = 'SENT', attempt_count = attempt_count + 1, provider_message_id = ?,
                    last_error = NULL, last_attempted_at = CURRENT_TIMESTAMP, sent_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status <> 'SENT'
                """, providerMessageId, attemptId);
    }

    public void markDeliveryFailed(UUID attemptId, String error) {
        jdbcTemplate.update("""
                UPDATE invoice_delivery_attempts
                SET status = 'FAILED', attempt_count = attempt_count + 1, last_error = ?,
                    last_attempted_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status <> 'SENT'
                """, error, attemptId);
    }

    private Optional<SalesInvoice> find(String whereClause, Object... arguments) {
        return jdbcTemplate.query("""
                SELECT i.*, s.user_id
                FROM sales_invoices i
                JOIN sales_orders s ON s.id = i.sales_order_id
                """ + whereClause, (resultSet, rowNum) -> map(resultSet), arguments).stream().findFirst();
    }

    private SalesInvoice map(ResultSet resultSet) throws SQLException {
        UUID invoiceId = resultSet.getObject("id", UUID.class);
        List<InvoiceLine> lines = jdbcTemplate.query("""
                SELECT id, line_number, sku_snapshot, description_snapshot, quantity,
                       unit_price_ex_gst_cents, gst_rate_basis_points, gst_cents, line_total_cents, taxable
                FROM sales_invoice_lines WHERE invoice_id = ? ORDER BY line_number
                """, (lineSet, rowNum) -> new InvoiceLine(
                lineSet.getObject("id", UUID.class), lineSet.getInt("line_number"),
                lineSet.getString("sku_snapshot"), lineSet.getString("description_snapshot"),
                lineSet.getInt("quantity"), lineSet.getLong("unit_price_ex_gst_cents"),
                lineSet.getInt("gst_rate_basis_points"), lineSet.getLong("gst_cents"),
                lineSet.getLong("line_total_cents"), lineSet.getBoolean("taxable")
        ), invoiceId);
        return new SalesInvoice(
                invoiceId,
                resultSet.getObject("sales_order_id", UUID.class),
                resultSet.getObject("payment_id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("invoice_number"),
                resultSet.getString("document_type"),
                resultSet.getString("status"),
                resultSet.getString("order_reference"),
                resultSet.getString("payment_reference"),
                resultSet.getString("seller_legal_name"),
                resultSet.getString("seller_trading_name"),
                resultSet.getString("seller_abn"),
                resultSet.getString("seller_address_line"),
                resultSet.getString("seller_email"),
                resultSet.getString("seller_phone"),
                resultSet.getString("buyer_name"),
                resultSet.getString("buyer_email"),
                resultSet.getString("buyer_customer_reference"),
                resultSet.getString("buyer_location"),
                resultSet.getString("buyer_abn"),
                resultSet.getString("currency").trim(),
                resultSet.getLong("subtotal_ex_gst_cents"),
                resultSet.getLong("gst_cents"),
                resultSet.getLong("total_cents"),
                resultSet.getLong("amount_paid_cents"),
                resultSet.getObject("issued_at", OffsetDateTime.class),
                resultSet.getObject("founder_number", Integer.class),
                resultSet.getString("founder_tier_name"),
                resultSet.getObject("founder_cashback_amount_cents", Long.class),
                resultSet.getString("founder_cashback_terms"),
                lines,
                resultSet.getBytes("pdf_content"),
                resultSet.getString("pdf_sha256")
        );
    }
}
