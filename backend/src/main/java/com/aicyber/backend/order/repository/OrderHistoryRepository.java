package com.aicyber.backend.order.repository;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.order.dto.OrderHistoryResponse;
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
                        "latest_payment.status AS payment_status " +
                        "FROM build_requests b LEFT JOIN sales_orders s ON s.build_request_id = b.id " +
                        "LEFT JOIN LATERAL (SELECT p.status FROM payments p WHERE p.order_id = s.id " +
                        "ORDER BY p.created_at DESC, p.id DESC LIMIT 1) latest_payment ON TRUE " +
                        "WHERE b.user_id = ? ORDER BY b.created_at DESC",
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
                    resultSet.getString("payment_status")
            );
        } catch (JsonProcessingException exception) {
            throw new SQLException("Order configuration is invalid", exception);
        }
    }
}
