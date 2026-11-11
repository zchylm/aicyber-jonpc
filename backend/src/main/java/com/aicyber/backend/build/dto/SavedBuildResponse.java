package com.aicyber.backend.build.dto;

import com.aicyber.backend.configurator.dto.ConfiguratorSelection;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SavedBuildResponse(
        UUID id,
        String name,
        String direction,
        String budgetRange,
        ConfiguratorSelection configuration,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) { }
