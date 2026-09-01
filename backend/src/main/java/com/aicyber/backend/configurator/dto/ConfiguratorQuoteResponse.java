package com.aicyber.backend.configurator.dto;

import java.util.List;

public record ConfiguratorQuoteResponse(
        int recommendedBaseline,
        int selectedAdjustments,
        int estimatedTotal,
        boolean compatible,
        String budgetStatus,
        List<String> validation
) {
}
