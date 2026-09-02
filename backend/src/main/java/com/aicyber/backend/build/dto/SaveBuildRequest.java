package com.aicyber.backend.build.dto;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;

public record SaveBuildRequest(
        String name,
        String direction,
        String budgetRange,
        int estimatedPrice,
        int recommendedBaseline,
        int selectedAdjustments,
        ConfiguratorQuoteRequest configuration
) { }
