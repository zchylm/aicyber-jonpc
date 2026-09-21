package com.aicyber.backend.order.repository;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.order.dto.OrderHistoryResponse;
import com.aicyber.backend.reward.dto.RewardEntryResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public class OrderHistoryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public OrderHistoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<OrderHistoryResponse> findAllByUserId(UUID userId) {
        return jdbcTemplate.query(
                "SELECT b.id, b.request_reference, b.direction, b.estimated_price, b.recommended_baseline, " +
                        "b.selected_adjustments, b.status, b.created_at, b.configuration_snapshot, " +
                        "s.id AS sales_order_id, s.order_reference, s.status AS order_status, " +
                        "latest_payment.payment_reference, latest_payment.status AS payment_status, " +
                        "i.id AS invoice_id, i.invoice_number, i.issued_at AS invoice_issued_at, " +
                        "q.id AS quote_id, q.status AS quote_status, q.total_cents AS quote_total_cents, " +
                        "q.valid_until AS quote_valid_until, q.review_note AS quote_note, " +
                        "c.id AS reward_id, c.founder_sequence, t.display_name AS reward_tier_name, " +
                        "c.rate_basis_points, c.cap_cents, c.purchase_amount_cents, c.cashback_amount_cents, " +
                        "c.status AS reward_status, c.locked_at, c.payable_at, c.payout_due_at, " +
                        "c.processing_at, c.paid_at, c.payout_method, c.payout_reference, c.payout_failure_reason " +
                        "FROM build_requests b LEFT JOIN sales_orders s ON s.build_request_id = b.id " +
                        "LEFT JOIN LATERAL (SELECT p.payment_reference, p.status FROM payments p WHERE p.order_id = s.id " +
                        "ORDER BY p.created_at DESC, p.id DESC LIMIT 1) latest_payment ON TRUE " +
                        "LEFT JOIN sales_invoices i ON i.sales_order_id = s.id " +
                        "LEFT JOIN LATERAL (SELECT id, status, total_cents, valid_until, review_note FROM custom_build_quotes " +
                        "WHERE build_request_id = b.id ORDER BY version DESC LIMIT 1) q ON TRUE " +
                        "LEFT JOIN reward_commitments c ON c.order_id = s.id AND c.status <> 'VOID' " +
                        "LEFT JOIN reward_founder_tiers t ON t.id = c.tier_id " +
                        "WHERE b.user_id = ? AND b.status <> 'CANCELLED' ORDER BY b.created_at DESC",
                (resultSet, rowNum) -> map(resultSet), userId
        );
    }

    private OrderHistoryResponse map(ResultSet resultSet) throws SQLException {
        try {
            return new OrderHistoryResponse(
                    resultSet.getObject("id", UUID.class),
                    resultSet.getString("request_reference"),
                    resultSet.getString("direction"),
                    resultSet.getInt("estimated_price"),
                    resultSet.getInt("recommended_baseline"),
                    resultSet.getInt("selected_adjustments"),
                    resultSet.getString("status"),
                    resultSet.getObject("created_at", OffsetDateTime.class),
                    objectMapper.readValue(resultSet.getString("configuration_snapshot"), ConfiguratorQuoteRequest.class),
                    resultSet.getObject("sales_order_id", UUID.class),
                    resultSet.getString("order_reference"),
                    resultSet.getString("order_status"),
                    resultSet.getString("payment_reference"),
                    resultSet.getString("payment_status"),
                    resultSet.getObject("invoice_id", UUID.class),
                    resultSet.getString("invoice_number"),
                    resultSet.getObject("invoice_issued_at", OffsetDateTime.class),
                    resultSet.getObject("quote_id", UUID.class),
                    resultSet.getString("quote_status"),
                    resultSet.getObject("quote_total_cents", Long.class),
                    resultSet.getObject("quote_valid_until", OffsetDateTime.class),
                    resultSet.getString("quote_note"),
                    mapReward(resultSet)
            );
        } catch (JsonProcessingException exception) {
            throw new SQLException("Order configuration is invalid", exception);
        }
    }

    private RewardEntryResponse mapReward(ResultSet resultSet) throws SQLException {
        UUID rewardId = resultSet.getObject("reward_id", UUID.class);
        if (rewardId == null) return null;
        long purchase = resultSet.getLong("purchase_amount_cents");
        long cashback = resultSet.getLong("cashback_amount_cents");
        return new RewardEntryResponse(
                rewardId, resultSet.getString("order_reference"), resultSet.getLong("founder_sequence"),
                resultSet.getString("reward_tier_name"), resultSet.getInt("rate_basis_points"),
                resultSet.getLong("cap_cents"), purchase, cashback, purchase - cashback,
                resultSet.getString("reward_status"), resultSet.getObject("locked_at", OffsetDateTime.class),
                resultSet.getObject("payable_at", OffsetDateTime.class),
                resultSet.getObject("payout_due_at", OffsetDateTime.class),
                resultSet.getObject("processing_at", OffsetDateTime.class),
                resultSet.getObject("paid_at", OffsetDateTime.class), resultSet.getString("payout_method"),
                resultSet.getString("payout_reference"), resultSet.getString("payout_failure_reason")
        );
    }
}
