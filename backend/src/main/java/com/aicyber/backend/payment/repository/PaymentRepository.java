package com.aicyber.backend.payment.repository;

import com.aicyber.backend.order.model.SalesOrder;
import com.aicyber.backend.payment.model.Payment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
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

    public Optional<Payment> findByOrderAndKey(UUID orderId, String idempotencyKey) {
        return query("WHERE p.order_id = ? AND p.idempotency_key = ?", orderId, idempotencyKey);
    }

    public Optional<Payment> findSuccessfulByOrder(UUID orderId) {
        return query("WHERE p.order_id = ? AND p.status = 'SUCCEEDED'", orderId);
    }

    public Optional<Payment> lockOwned(UUID paymentId, UUID userId) {
        return query("WHERE p.id = ? AND s.user_id = ? FOR UPDATE OF p, s", paymentId, userId);
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

    private Optional<Payment> findById(UUID paymentId) {
        return query("WHERE p.id = ?", paymentId);
    }

    private Optional<Payment> query(String whereClause, Object... arguments) {
        return jdbcTemplate.query(
                "SELECT p.id, p.order_id, s.order_reference, s.user_id, p.payment_reference, p.provider, " +
                        "p.amount_cents, p.currency, s.status AS order_status, p.status, p.idempotency_key, " +
                        "p.failure_reason, p.succeeded_at " +
                        "FROM payments p JOIN sales_orders s ON s.id = p.order_id " + whereClause,
                (resultSet, rowNumber) -> map(resultSet),
                arguments
        ).stream().findFirst();
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
                resultSet.getObject("succeeded_at", OffsetDateTime.class)
        );
    }

    private void requireSingleUpdate(int updatedRows, String message) {
        if (updatedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
