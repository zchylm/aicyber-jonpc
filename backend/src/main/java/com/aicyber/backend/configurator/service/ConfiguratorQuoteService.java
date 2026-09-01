package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConfiguratorQuoteService {

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
        int recommendedMemoryPrice = ConfiguratorCatalog.MEMORY_PRICES.get(request.recommendedMemoryId());
        int recommendedStoragePrice = ConfiguratorCatalog.STORAGE_PRICES.get(request.recommendedStorageId());
        int recommendedMotherboardPrice = ConfiguratorCatalog.MOTHERBOARD_PRICES.get(request.recommendedMotherboardId());
        int recommendedPsuPrice = ConfiguratorCatalog.PSU_PRICES.get(request.recommendedPsuId());
        int recommendedCasePrice = ConfiguratorCatalog.CASE_PRICES.get(request.recommendedCaseId());
        int recommendedCoolingPrice = ConfiguratorCatalog.COOLING_PRICES.get(request.recommendedCoolingId());

        int baseline = ConfiguratorCatalog.SYSTEM_BASE_PRICE + recommendedCpuPrice + recommendedGpuPrice;
        int selectedTotal = ConfiguratorCatalog.SYSTEM_BASE_PRICE + ConfiguratorCatalog.CPU_PRICES.get(request.cpuId()) + ConfiguratorCatalog.GPU_PRICES.get(request.gpuId())
                + (ConfiguratorCatalog.MEMORY_PRICES.get(request.memoryId()) - recommendedMemoryPrice)
                + (ConfiguratorCatalog.STORAGE_PRICES.get(request.storageId()) - recommendedStoragePrice)
                + (ConfiguratorCatalog.MOTHERBOARD_PRICES.get(request.motherboardId()) - recommendedMotherboardPrice)
                + (ConfiguratorCatalog.PSU_PRICES.get(request.psuId()) - recommendedPsuPrice)
                + (ConfiguratorCatalog.CASE_PRICES.get(request.caseId()) - recommendedCasePrice)
                + (ConfiguratorCatalog.COOLING_PRICES.get(request.coolingId()) - recommendedCoolingPrice);
        int adjustments = selectedTotal - baseline;

        List<String> validation = validate(request);
        String budgetStatus = budgetStatusLabel(request, selectedTotal);
        return new ConfiguratorQuoteResponse(baseline, adjustments, selectedTotal,
                validation.isEmpty(), budgetStatus, validation);
    }

    private List<String> validate(ConfiguratorQuoteRequest request) {
        String cpuPlatform = request.cpuId().startsWith("core-") ? "LGA1700" : "AM5";
        boolean intelBoard = request.motherboardId().startsWith("b760") || request.motherboardId().startsWith("z790");
        boolean motherboardMatches = ("LGA1700".equals(cpuPlatform) && intelBoard)
                || ("AM5".equals(cpuPlatform) && !intelBoard);
        int minimumPsu = switch (request.gpuId()) {
            case "rtx-5080" -> 850;
            case "rtx-5070", "rx-7800-xt" -> 750;
            case "rtx-4060", "arc-b580" -> 550;
            default -> 450;
        };
        int psuWattage = psuWattage(request.psuId());
        boolean compactCase = "compact".equals(request.caseId());
        boolean microAtxBoard = "b650m-no-wifi".equals(request.motherboardId());
        boolean caseMatches = !compactCase || microAtxBoard;
        boolean coolingMatches = switch (request.coolingId()) {
            case "tower-air" -> !highHeatCpu(request.cpuId()) && !"full".equals(request.caseId());
            case "dual-tower-air", "240-liquid" -> !"compact".equals(request.caseId());
            case "360-liquid" -> "full".equals(request.caseId());
            default -> false;
        };

        return java.util.stream.Stream.of(
                        motherboardMatches ? null : "Selected motherboard does not match the CPU platform.",
                        psuWattage >= minimumPsu ? null : "Selected PSU wattage is below the GPU power requirement.",
                        caseMatches ? null : "Compact case requires the Micro-ATX motherboard option.",
                        coolingMatches ? null : "Selected cooling option is not supported by the CPU and case combination."
                )
                .filter(java.util.Objects::nonNull)
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

    private int psuWattage(String id) { return Integer.parseInt(id.substring(0, id.indexOf('-'))); }
    private boolean highHeatCpu(String id) { return "ryzen-9-7900".equals(id) || "core-i7-14700k".equals(id); }

    private void requireKnown(String id, java.util.Map<String, Integer> options, String label) {
        if (id == null || !options.containsKey(id)) throw new IllegalArgumentException("Unknown " + label + " option");
    }
}
