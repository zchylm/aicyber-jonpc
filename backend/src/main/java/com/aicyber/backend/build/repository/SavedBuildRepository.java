package com.aicyber.backend.build.repository;

import com.aicyber.backend.build.dto.SaveBuildRequest;
import com.aicyber.backend.build.dto.SavedBuildResponse;
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
public class SavedBuildRepository {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SavedBuildRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public SavedBuildResponse create(UUID userId, SaveBuildRequest request) {
        validate(request);
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO saved_builds (id, user_id, name, direction, budget_range, estimated_price, " +
                        "recommended_baseline, selected_adjustments, configuration_snapshot) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)",
                id, userId, request.name().trim(), request.direction(), request.budgetRange(), request.estimatedPrice(),
                request.recommendedBaseline(), request.selectedAdjustments(), toJson(request.configuration())
        );
        return findById(userId, id);
    }

    public SavedBuildResponse update(UUID userId, UUID id, SaveBuildRequest request) {
        validate(request);
        int updated = jdbcTemplate.update(
                "UPDATE saved_builds SET name = ?, direction = ?, budget_range = ?, estimated_price = ?, " +
                        "recommended_baseline = ?, selected_adjustments = ?, configuration_snapshot = ?::jsonb, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ? AND status = 'ACTIVE'",
                request.name().trim(), request.direction(), request.budgetRange(), request.estimatedPrice(),
                request.recommendedBaseline(), request.selectedAdjustments(), toJson(request.configuration()), id, userId
        );
        if (updated == 0) throw new IllegalArgumentException("Saved build was not found");
        return findById(userId, id);
    }

    public List<SavedBuildResponse> findAllByUserId(UUID userId) {
        return jdbcTemplate.query(
                "SELECT id, name, direction, budget_range, estimated_price, recommended_baseline, selected_adjustments, " +
                        "configuration_snapshot, status, created_at, updated_at FROM saved_builds " +
                        "WHERE user_id = ? AND status = 'ACTIVE' ORDER BY updated_at DESC",
                (resultSet, rowNum) -> map(resultSet), userId
        );
    }

    public void delete(UUID userId, UUID buildId) {
        int deleted = jdbcTemplate.update("DELETE FROM saved_builds WHERE id = ? AND user_id = ?", buildId, userId);
        if (deleted == 0) throw new IllegalArgumentException("Saved build was not found");
    }

    private SavedBuildResponse findById(UUID userId, UUID id) {
        return jdbcTemplate.query(
                "SELECT id, name, direction, budget_range, estimated_price, recommended_baseline, selected_adjustments, " +
                        "configuration_snapshot, status, created_at, updated_at FROM saved_builds WHERE id = ? AND user_id = ?",
                (resultSet, rowNum) -> map(resultSet), id, userId
        ).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Saved build was not found"));
    }

    private SavedBuildResponse map(ResultSet resultSet) throws SQLException {
        try {
            return new SavedBuildResponse(
                    resultSet.getObject("id", UUID.class), resultSet.getString("name"), resultSet.getString("direction"),
                    resultSet.getString("budget_range"), resultSet.getInt("estimated_price"),
                    resultSet.getInt("recommended_baseline"), resultSet.getInt("selected_adjustments"),
                    objectMapper.readValue(resultSet.getString("configuration_snapshot"), com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest.class),
                    resultSet.getString("status"), resultSet.getObject("created_at", OffsetDateTime.class),
                    resultSet.getObject("updated_at", OffsetDateTime.class)
            );
        } catch (JsonProcessingException exception) {
            throw new SQLException("Saved build configuration is invalid", exception);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Build configuration could not be saved", exception);
        }
    }

    private void validate(SaveBuildRequest request) {
        if (request == null || request.configuration() == null) throw new IllegalArgumentException("Build configuration is required");
        if (request.name() == null || request.name().isBlank()) throw new IllegalArgumentException("Build name is required");
        if (request.name().trim().length() > 160) throw new IllegalArgumentException("Build name is too long");
        if (request.estimatedPrice() < 0 || request.recommendedBaseline() < 0) throw new IllegalArgumentException("Build price is invalid");
    }
}
