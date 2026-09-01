package com.aicyber.backend.configurator.dto;

import java.util.Map;

public record ConfiguratorQuoteRequest(
        String direction,
        Map<String, String> answers,
        String recommendedCpuId,
        String recommendedGpuId,
        String recommendedMemoryId,
        String recommendedStorageId,
        String recommendedMotherboardId,
        String recommendedPsuId,
        String recommendedCaseId,
        String recommendedCoolingId,
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
