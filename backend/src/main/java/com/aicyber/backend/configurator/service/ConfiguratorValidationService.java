package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorSelection;
import com.aicyber.backend.configurator.dto.ConfiguratorValidationResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguratorValidationService {

    private final ConfiguratorCompatibilityService compatibilityService = new ConfiguratorCompatibilityService();

    public ConfiguratorValidationResponse validate(ConfiguratorSelection request) {
        if (request == null) {
            throw new IllegalArgumentException("Configuration must not be null");
        }

        requireKnown(request.cpuId(), "cpu", "CPU");
        requireKnown(request.gpuId(), "gpu", "GPU");
        requireKnown(request.memoryId(), "memory", "memory");
        requireKnown(request.storageId(), "storage", "storage");
        requireKnown(request.motherboardId(), "motherboard", "motherboard");
        requireKnown(request.psuId(), "psu", "PSU");
        requireKnown(request.caseId(), "case", "case");
        requireKnown(request.coolingId(), "cooling", "cooling");

        requireKnown(request.recommendedCpuId(), "cpu", "recommended CPU");
        requireKnown(request.recommendedGpuId(), "gpu", "recommended GPU");
        requireKnown(request.recommendedMemoryId(), "memory", "recommended memory");
        requireKnown(request.recommendedStorageId(), "storage", "recommended storage");
        requireKnown(request.recommendedMotherboardId(), "motherboard", "recommended motherboard");
        requireKnown(request.recommendedPsuId(), "psu", "recommended PSU");
        requireKnown(request.recommendedCaseId(), "case", "recommended case");
        requireKnown(request.recommendedCoolingId(), "cooling", "recommended cooling");

        List<String> validation = compatibilityValidation(request);
        return new ConfiguratorValidationResponse(validation.isEmpty(), validation);
    }

    private List<String> compatibilityValidation(ConfiguratorSelection request) {
        List<String> compatibility = compatibilityService.compatibleOptions(new com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityRequest(
                request.cpuId(), request.gpuId(), request.motherboardId(), request.psuId(), request.caseId(), request.coolingId()
        )).validation();
        if (request.caseColorId() != null && List.of("black", "white", "no-preference").contains(request.caseColorId())) return compatibility;
        return java.util.stream.Stream.concat(compatibility.stream(), java.util.stream.Stream.of("Selected case finish is not available."))
                .toList();
    }

    private void requireKnown(String id, String category, String label) {
        if (id == null || !ConfiguratorCatalog.optionIds(category).contains(id))
            throw new IllegalArgumentException("Unknown " + label + " option");
    }
}
