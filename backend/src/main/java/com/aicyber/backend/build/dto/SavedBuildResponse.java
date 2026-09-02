package com.aicyber.backend.build.dto;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SavedBuildResponse(
        UUID id,
        String name,
        String direction,
        String budgetRange,
        int estimatedPrice,
        int recommendedBaseline,
        int selectedAdjustments,
        ConfiguratorQuoteRequest configuration,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) { }
