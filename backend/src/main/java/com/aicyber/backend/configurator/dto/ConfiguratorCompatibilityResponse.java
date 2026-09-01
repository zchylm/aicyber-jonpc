package com.aicyber.backend.configurator.dto;

import java.util.List;

public record ConfiguratorCompatibilityResponse(
        List<String> motherboardIds,
        List<String> psuIds,
        List<String> caseIds,
        List<String> coolingIds,
        List<String> validation
) {
}
