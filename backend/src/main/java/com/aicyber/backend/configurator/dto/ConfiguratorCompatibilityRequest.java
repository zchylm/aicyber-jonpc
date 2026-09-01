package com.aicyber.backend.configurator.dto;

public record ConfiguratorCompatibilityRequest(
        String cpuId,
        String gpuId,
        String motherboardId,
        String psuId,
        String caseId,
        String coolingId
) {
}
