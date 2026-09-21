package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguratorQuoteService {

    private final ConfiguratorCompatibilityService compatibilityService = new ConfiguratorCompatibilityService();

    public ConfiguratorQuoteResponse quote(ConfiguratorQuoteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Quote request must not be null");
        }

        requireKnown(request.cpuId(), ConfiguratorCatalog.CPU_PRICES, "CPU");
        requireKnown(request.gpuId(), ConfiguratorCatalog.GPU_PRICES, "GPU");
        requireKnown(request.memoryId(), ConfiguratorCatalog.MEMORY_PRICES, "memory");
        requireKnown(request.storageId(), ConfiguratorCatalog.STORAGE_PRICES, "storage");
        requireKnown(request.motherboardId(), ConfiguratorCatalog.MOTHERBOARD_PRICES, "motherboard");
        requireKnown(request.psuId(), ConfiguratorCatalog.PSU_PRICES, "PSU");
        requireKnown(request.caseId(), ConfiguratorCatalog.CASE_PRICES, "case");
        requireKnown(request.coolingId(), ConfiguratorCatalog.COOLING_PRICES, "cooling");

        requireKnown(request.recommendedCpuId(), ConfiguratorCatalog.CPU_PRICES, "recommended CPU");
        requireKnown(request.recommendedGpuId(), ConfiguratorCatalog.GPU_PRICES, "recommended GPU");
        requireKnown(request.recommendedMemoryId(), ConfiguratorCatalog.MEMORY_PRICES, "recommended memory");
        requireKnown(request.recommendedStorageId(), ConfiguratorCatalog.STORAGE_PRICES, "recommended storage");
        requireKnown(request.recommendedMotherboardId(), ConfiguratorCatalog.MOTHERBOARD_PRICES, "recommended motherboard");
        requireKnown(request.recommendedPsuId(), ConfiguratorCatalog.PSU_PRICES, "recommended PSU");
        requireKnown(request.recommendedCaseId(), ConfiguratorCatalog.CASE_PRICES, "recommended case");
        requireKnown(request.recommendedCoolingId(), ConfiguratorCatalog.COOLING_PRICES, "recommended cooling");

        int recommendedCpuPrice = ConfiguratorCatalog.CPU_PRICES.get(request.recommendedCpuId());
        int recommendedGpuPrice = ConfiguratorCatalog.GPU_PRICES.get(request.recommendedGpuId());
        int baseline = ConfiguratorCatalog.SYSTEM_BASE_PRICE + recommendedCpuPrice + recommendedGpuPrice
                + ConfiguratorCatalog.MEMORY_PRICES.get(request.recommendedMemoryId())
                + ConfiguratorCatalog.STORAGE_PRICES.get(request.recommendedStorageId())
                + ConfiguratorCatalog.MOTHERBOARD_PRICES.get(request.recommendedMotherboardId())
                + ConfiguratorCatalog.PSU_PRICES.get(request.recommendedPsuId())
                + ConfiguratorCatalog.CASE_PRICES.get(request.recommendedCaseId())
                + ConfiguratorCatalog.COOLING_PRICES.get(request.recommendedCoolingId());
        int selectedTotal = ConfiguratorCatalog.SYSTEM_BASE_PRICE + ConfiguratorCatalog.CPU_PRICES.get(request.cpuId()) + ConfiguratorCatalog.GPU_PRICES.get(request.gpuId())
                + ConfiguratorCatalog.MEMORY_PRICES.get(request.memoryId())
                + ConfiguratorCatalog.STORAGE_PRICES.get(request.storageId())
                + ConfiguratorCatalog.MOTHERBOARD_PRICES.get(request.motherboardId())
                + ConfiguratorCatalog.PSU_PRICES.get(request.psuId())
                + ConfiguratorCatalog.CASE_PRICES.get(request.caseId())
                + ConfiguratorCatalog.COOLING_PRICES.get(request.coolingId());
        int adjustments = selectedTotal - baseline;

        List<String> validation = validate(request);
        String budgetStatus = budgetStatusLabel(request, selectedTotal);
        return new ConfiguratorQuoteResponse(baseline, adjustments, selectedTotal,
                validation.isEmpty(), budgetStatus, validation);
    }

    private List<String> validate(ConfiguratorQuoteRequest request) {
        List<String> compatibility = compatibilityService.compatibleOptions(new com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityRequest(
                request.cpuId(), request.gpuId(), request.motherboardId(), request.psuId(), request.caseId(), request.coolingId()
        )).validation();
        if (List.of("black", "white", "no-preference").contains(request.caseColorId())) return compatibility;
        return java.util.stream.Stream.concat(compatibility.stream(), java.util.stream.Stream.of("Selected case finish is not available."))
                .toList();
    }

    private int budgetStatusCode(ConfiguratorQuoteRequest request, int total) {
        String budget = request.answers() == null ? "" : request.answers().getOrDefault("budget", "");
        int[] range = parseBudget(budget);
        if (total < range[0]) return -1;
        if (total > range[1]) return 1;
        return 0;
    }

    private String budgetStatusLabel(ConfiguratorQuoteRequest request, int total) {
        return switch (budgetStatusCode(request, total)) {
            case -1 -> "Below selected range";
            case 1 -> "Above selected budget";
            default -> "Within selected budget";
        };
    }

    private int[] parseBudget(String value) {
        if (value != null) {
            String[] values = value.replace("$", "").replace(",", "").split("[–-]");
            if (values.length == 2) {
                try { return new int[]{Integer.parseInt(values[0]), Integer.parseInt(values[1])}; }
                catch (NumberFormatException ignored) { }
            }
        }
        return new int[]{0, Integer.MAX_VALUE};
    }

    private void requireKnown(String id, java.util.Map<String, Integer> options, String label) {
        if (id == null || !options.containsKey(id)) throw new IllegalArgumentException("Unknown " + label + " option");
    }
}
