package com.aicyber.backend.configurator.dto;

public record ConfiguratorRecommendationResponse(
        String cpuId,
        String gpuId,
        String memoryId,
        String storageId,
        String motherboardId,
        String psuId,
        String caseId,
        String coolingId
) {
}
