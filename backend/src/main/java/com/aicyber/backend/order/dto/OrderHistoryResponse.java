package com.aicyber.backend.order.dto;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderHistoryResponse(
        UUID id,
        String requestReference,
        String direction,
        int estimatedPrice,
        int recommendedBaseline,
        int selectedAdjustments,
        String status,
        OffsetDateTime createdAt,
        ConfiguratorQuoteRequest configuration
) {
}
