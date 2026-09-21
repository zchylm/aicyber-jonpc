package com.aicyber.backend.order.repository;

import com.aicyber.backend.order.model.BuildRequestOrderSource;
import com.aicyber.backend.order.model.SalesOrder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public class SalesOrderRepository {
    private final JdbcTemplate jdbcTemplate;

    public SalesOrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<BuildRequestOrderSource> findLatestUnconvertedBuildRequest(UUID userId) {
        return jdbcTemplate.query(
                "SELECT b.id, b.user_id, b.request_reference, " +
                        "COALESCE(q.total_cents, b.estimated_price * 100::bigint) AS price_cents " +
                        "FROM build_requests b LEFT JOIN sales_orders s ON s.build_request_id = b.id " +
                        "LEFT JOIN custom_build_quotes q ON q.build_request_id = b.id AND q.status = 'ACCEPTED' " +
                        "WHERE b.user_id = ? AND b.status <> 'CANCELLED' AND s.id IS NULL " +
                        "AND (b.configuration_snapshot->'answers'->>'systemSku' IS NOT NULL OR q.id IS NOT NULL) " +
                        "ORDER BY b.created_at DESC LIMIT 1",
                (resultSet, rowNumber) -> new BuildRequestOrderSource(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getString("request_reference"),
                        resultSet.getLong("price_cents")
                ),
                userId
        ).stream().findFirst();
    }

    public Optional<BuildRequestOrderSource> findBuildRequest(UUID userId, String requestReference) {
        return jdbcTemplate.query(
                "SELECT b.id, b.user_id, b.request_reference, " +
                        "COALESCE(q.total_cents, b.estimated_price * 100::bigint) AS price_cents " +
                        "FROM build_requests b " +
                        "LEFT JOIN custom_build_quotes q ON q.build_request_id = b.id AND q.status = 'ACCEPTED' " +
                        "WHERE b.user_id = ? AND b.request_reference = ? AND b.status <> 'CANCELLED' " +
                        "AND (b.configuration_snapshot->'answers'->>'systemSku' IS NOT NULL OR q.id IS NOT NULL)",
                (resultSet, rowNumber) -> new BuildRequestOrderSource(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("user_id", UUID.class),
                        resultSet.getString("request_reference"),
                        resultSet.getLong("price_cents")
                ),
                userId,
                requestReference
        ).stream().findFirst();
    }

    public SalesOrder createFromBuildRequest(BuildRequestOrderSource source, String orderReference) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO sales_orders " +
                        "(id, user_id, build_request_id, order_reference, amount_cents, currency, status) " +
                        "VALUES (?, ?, ?, ?, ?, 'AUD', 'PENDING_PAYMENT') " +
                        "ON CONFLICT (build_request_id) DO NOTHING",
                orderId,
                source.userId(),
                source.id(),
                orderReference,
                source.estimatedPriceCents()
        );
        SalesOrder order = findByBuildRequestId(source.id()).orElseThrow();
        jdbcTemplate.update("""
                INSERT INTO sales_order_delivery
                    (sales_order_id, recipient_name, phone, address_line_1, address_line_2, suburb, state, postcode, country)
                SELECT ?, recipient_name, phone, address_line_1, address_line_2, suburb, state, postcode, country
                FROM build_delivery_details WHERE build_request_id = ?
                ON CONFLICT (sales_order_id) DO NOTHING
                """, order.id(), source.id());
        return order;
    }

    public SalesOrder createDirect(UUID userId, long amountCents, String orderReference) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO sales_orders " +
                        "(id, user_id, order_reference, amount_cents, currency, status) " +
                        "VALUES (?, ?, ?, ?, 'AUD', 'PENDING_PAYMENT')",
                orderId,
                userId,
                orderReference,
                amountCents
        );
        return findById(orderId).orElseThrow();
    }

    public SalesOrder markPaid(UUID orderId) {
        int updated = jdbcTemplate.update(
                "UPDATE sales_orders SET status = 'PAID', paid_at = CURRENT_TIMESTAMP, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PENDING_PAYMENT'",
                orderId
        );
        requireSingleUpdate(updated, "Sales order was not pending payment");
        return findById(orderId).orElseThrow();
    }

    public SalesOrder markRewardEligible(UUID orderId) {
        int updated = jdbcTemplate.update(
                "UPDATE sales_orders SET status = 'REWARD_ELIGIBLE', reward_eligible_at = CURRENT_TIMESTAMP, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PAID'",
                orderId
        );
        requireSingleUpdate(updated, "Sales order was not paid");
        return findById(orderId).orElseThrow();
    }

    public Optional<SalesOrder> findById(UUID orderId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, build_request_id, order_reference, amount_cents, currency, status, " +
                        "paid_at, reward_eligible_at FROM sales_orders WHERE id = ?",
                (resultSet, rowNumber) -> map(resultSet),
                orderId
        ).stream().findFirst();
    }

    public Optional<SalesOrder> findByBuildRequestId(UUID buildRequestId) {
        return jdbcTemplate.query(
                "SELECT id, user_id, build_request_id, order_reference, amount_cents, currency, status, " +
                        "paid_at, reward_eligible_at FROM sales_orders WHERE build_request_id = ?",
                (resultSet, rowNumber) -> map(resultSet),
                buildRequestId
        ).stream().findFirst();
    }

    private SalesOrder map(ResultSet resultSet) throws SQLException {
        return new SalesOrder(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("user_id", UUID.class),
                resultSet.getObject("build_request_id", UUID.class),
                resultSet.getString("order_reference"),
                resultSet.getLong("amount_cents"),
                resultSet.getString("currency").trim(),
                resultSet.getString("status"),
                resultSet.getObject("paid_at", OffsetDateTime.class),
                resultSet.getObject("reward_eligible_at", OffsetDateTime.class)
        );
    }

    private void requireSingleUpdate(int updatedRows, String message) {
        if (updatedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
