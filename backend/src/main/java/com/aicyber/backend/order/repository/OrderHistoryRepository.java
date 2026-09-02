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
                "SELECT id, request_reference, direction, estimated_price, recommended_baseline, " +
                        "selected_adjustments, status, created_at, configuration_snapshot " +
                        "FROM build_requests WHERE user_id = ? ORDER BY created_at DESC",
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
                    objectMapper.readValue(resultSet.getString("configuration_snapshot"), ConfiguratorQuoteRequest.class)
            );
        } catch (JsonProcessingException exception) {
            throw new SQLException("Order configuration is invalid", exception);
        }
    }
}
