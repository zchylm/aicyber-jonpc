package com.aicyber.backend.payment.repository;

import com.aicyber.backend.order.model.SalesOrder;
import com.aicyber.backend.payment.model.Payment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;
import java.util.UUID;

@Repository
public class PaymentRepository {
    private final JdbcTemplate jdbcTemplate;

    public PaymentRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Payment create(SalesOrder order, String paymentReference, String idempotencyKey) {
        UUID paymentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO payments " +
                        "(id, order_id, payment_reference, provider, amount_cents, currency, status, idempotency_key) " +
                        "VALUES (?, ?, ?, 'MOCK', ?, ?, 'CREATED', ?) " +
                        "ON CONFLICT (order_id, idempotency_key) DO NOTHING",
                paymentId,
                order.id(),
                paymentReference,
                order.amountCents(),
                order.currency(),
                idempotencyKey
        );
        return findByOrderAndKey(order.id(), idempotencyKey).orElseThrow();
    }

    public Payment createBankTransfer(
            SalesOrder order,
            String paymentReference,
            String idempotencyKey,
            OffsetDateTime expiresAt
    ) {
        UUID paymentId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO payments " +
                        "(id, order_id, payment_reference, provider, amount_cents, currency, status, idempotency_key, expires_at) " +
                        "VALUES (?, ?, ?, 'BANK_TRANSFER', ?, ?, 'CREATED', ?, ?) " +
                        "ON CONFLICT (order_id, idempotency_key) DO NOTHING",
                paymentId, order.id(), paymentReference, order.amountCents(), order.currency(), idempotencyKey, expiresAt
        );
        return findByOrderAndKey(order.id(), idempotencyKey).orElseThrow();
    }

    public Optional<Payment> findByOrderAndKey(UUID orderId, String idempotencyKey) {
        return query("WHERE p.order_id = ? AND p.idempotency_key = ?", orderId, idempotencyKey);
    }

    public Optional<Payment> findSuccessfulByOrder(UUID orderId) {
        return query("WHERE p.order_id = ? AND p.status = 'SUCCEEDED'", orderId);
    }

    public Optional<Payment> findActiveBankTransferByBuildRequest(UUID userId, String requestReference) {
        return query("JOIN build_requests b ON b.id = s.build_request_id " +
                "WHERE s.user_id = ? AND b.request_reference = ? AND p.provider = 'BANK_TRANSFER' " +
                "AND p.status IN ('CREATED', 'PROCESSING') ORDER BY p.created_at DESC LIMIT 1",
                userId, requestReference);
    }

    public Optional<Payment> lockOwned(UUID paymentId, UUID userId) {
        return query("WHERE p.id = ? AND s.user_id = ? FOR UPDATE OF p, s", paymentId, userId);
    }

    public Optional<Payment> findOwned(UUID paymentId, UUID userId) {
        return query("WHERE p.id = ? AND s.user_id = ?", paymentId, userId);
    }

    public Optional<Payment> lock(UUID paymentId) {
        return query("WHERE p.id = ? FOR UPDATE OF p, s", paymentId);
    }

    public Payment markCustomerReported(UUID paymentId) {
        int updated = jdbcTemplate.update(
                "UPDATE payments SET status = 'PROCESSING', customer_reported_at = CURRENT_TIMESTAMP, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND provider = 'BANK_TRANSFER' " +
                        "AND status = 'CREATED' AND expires_at > CURRENT_TIMESTAMP",
                paymentId
        );
        requireSingleUpdate(updated, "Bank transfer is no longer awaiting confirmation");
        return findById(paymentId).orElseThrow();
    }

    public Payment markBankSucceeded(
            UUID paymentId,
            UUID adminId,
            String bankTransactionReference,
            OffsetDateTime bankReceivedAt,
            String reviewNote
    ) {
        int updated = jdbcTemplate.update(
                "UPDATE payments SET status = 'SUCCEEDED', provider_payment_id = ?, confirmed_by = ?, " +
                        "confirmed_at = CURRENT_TIMESTAMP, bank_received_at = ?, review_note = ?, failure_reason = NULL, " +
                        "succeeded_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                        "WHERE id = ? AND provider = 'BANK_TRANSFER' AND status = 'PROCESSING'",
                bankTransactionReference, adminId, bankReceivedAt, reviewNote, paymentId
        );
        requireSingleUpdate(updated, "Bank transfer was not ready to confirm");
        return findById(paymentId).orElseThrow();
    }

    public Payment markBankRejected(UUID paymentId, UUID adminId, String reviewNote) {
        int updated = jdbcTemplate.update(
                "UPDATE payments SET status = 'FAILED', confirmed_by = ?, confirmed_at = CURRENT_TIMESTAMP, " +
                        "review_note = ?, failure_reason = 'No matching funds found in ANZ', " +
                        "updated_at = CURRENT_TIMESTAMP " +
                        "WHERE id = ? AND provider = 'BANK_TRANSFER' AND status = 'PROCESSING'",
                adminId, reviewNote, paymentId
        );
        requireSingleUpdate(updated, "Bank transfer was not ready to reject");
        return findById(paymentId).orElseThrow();
    }

    public void lockBankSettlementSequence() {
        jdbcTemplate.queryForObject("SELECT pg_advisory_xact_lock(74102501)", Object.class);
    }

    public Optional<OffsetDateTime> latestBankReceivedAt() {
        return Optional.ofNullable(jdbcTemplate.queryForObject(
                "SELECT MAX(bank_received_at) FROM payments " +
                        "WHERE provider = 'BANK_TRANSFER' AND status = 'SUCCEEDED'",
                OffsetDateTime.class
        ));
    }

    public void cancelOtherAttempts(UUID orderId, UUID successfulPaymentId) {
        jdbcTemplate.update(
                "UPDATE payments SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP " +
                        "WHERE order_id = ? AND id <> ? AND status IN ('CREATED', 'PROCESSING')",
                orderId, successfulPaymentId
        );
    }

    public List<Payment> findExpiredBankTransfers() {
        return queryMany("WHERE p.provider = 'BANK_TRANSFER' AND p.status = 'CREATED' " +
                "AND p.expires_at <= CURRENT_TIMESTAMP ORDER BY p.expires_at LIMIT 100");
    }

    public void cancelExpired(UUID paymentId) {
        jdbcTemplate.update(
                "UPDATE payments SET status = 'CANCELLED', failure_reason = 'Bank transfer window expired', " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND provider = 'BANK_TRANSFER' AND status = 'CREATED'",
                paymentId
        );
    }

    public Payment markSucceeded(UUID paymentId) {
        int updated = jdbcTemplate.update(
                "UPDATE payments SET status = 'SUCCEEDED', failure_reason = NULL, " +
                        "succeeded_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP " +
                        "WHERE id = ? AND status = 'CREATED'",
                paymentId
        );
        requireSingleUpdate(updated, "Payment was not ready to complete");
        return findById(paymentId).orElseThrow();
    }

    public Payment markFailed(UUID paymentId, String reason) {
        int updated = jdbcTemplate.update(
                "UPDATE payments SET status = 'FAILED', failure_reason = ?, updated_at = CURRENT_TIMESTAMP " +
                        "WHERE id = ? AND status = 'CREATED'",
                reason,
                paymentId
        );
        requireSingleUpdate(updated, "Payment was not ready to decline");
        return findById(paymentId).orElseThrow();
    }

    public Optional<Payment> findById(UUID paymentId) {
        return query("WHERE p.id = ?", paymentId);
    }

    private Optional<Payment> query(String whereClause, Object... arguments) {
        return queryMany(whereClause, arguments).stream().findFirst();
    }

    private List<Payment> queryMany(String whereClause, Object... arguments) {
        return jdbcTemplate.query(
                "SELECT p.id, p.order_id, s.order_reference, s.user_id, p.payment_reference, p.provider, " +
                        "p.amount_cents, p.currency, s.status AS order_status, p.status, p.idempotency_key, " +
                        "p.failure_reason, p.succeeded_at, p.expires_at, p.customer_reported_at, p.bank_received_at, " +
                        "p.confirmed_by, p.confirmed_at, p.review_note " +
                        "FROM payments p JOIN sales_orders s ON s.id = p.order_id " + whereClause,
                (resultSet, rowNumber) -> map(resultSet),
                arguments
        );
    }

    private Payment map(ResultSet resultSet) throws SQLException {
        return new Payment(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("order_id", UUID.class),
                resultSet.getString("order_reference"),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("payment_reference"),
                resultSet.getString("provider"),
                resultSet.getLong("amount_cents"),
                resultSet.getString("currency").trim(),
                resultSet.getString("order_status"),
                resultSet.getString("status"),
                resultSet.getString("idempotency_key"),
                resultSet.getString("failure_reason"),
                resultSet.getObject("succeeded_at", OffsetDateTime.class),
                resultSet.getObject("expires_at", OffsetDateTime.class),
                resultSet.getObject("customer_reported_at", OffsetDateTime.class),
                resultSet.getObject("bank_received_at", OffsetDateTime.class),
                resultSet.getObject("confirmed_by", UUID.class),
                resultSet.getObject("confirmed_at", OffsetDateTime.class),
                resultSet.getString("review_note")
        );
    }

    private void requireSingleUpdate(int updatedRows, String message) {
        if (updatedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
