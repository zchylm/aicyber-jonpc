package com.aicyber.backend.configurator.repository;

import com.aicyber.backend.configurator.dto.ConfiguratorBuildRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteResponse;
import com.aicyber.backend.configurator.service.BuildRequestStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class BuildRequestRepository implements BuildRequestStore {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public BuildRequestRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public UUID create(UUID userId, ConfiguratorBuildRequest request, ConfiguratorQuoteResponse quote, String reference) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO build_requests (id, user_id, request_reference, name, email, phone, location, notes, " +
                        "contact_requested, direction, estimated_price, recommended_baseline, selected_adjustments, " +
                        "configuration_snapshot) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)",
                id, userId, reference, request.name().trim(), request.email().trim(), nullable(request.phone()),
                request.location().trim(), nullable(request.notes()), request.contact(), request.configuration().direction(),
                quote.estimatedTotal(), quote.recommendedBaseline(), quote.selectedAdjustments(), toJson(request.configuration())
        );
        return id;
    }

    private String nullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Build request configuration could not be saved", exception);
        }
    }
}
