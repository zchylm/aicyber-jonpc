package com.aicyber.backend.configurator.dto;

import java.util.Map;

public record ConfiguratorRecommendationRequest(
        String direction,
        Map<String, String> answers
) {
}
