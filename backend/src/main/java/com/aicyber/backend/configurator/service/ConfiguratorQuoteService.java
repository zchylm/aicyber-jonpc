package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConfiguratorQuoteService {

    private static final Map<String, Integer> CPU_PRICES = Map.of(
            "ryzen-5-7600", 299,
            "core-i5-14600k", 399,
            "ryzen-7-7700", 449,
            "ryzen-7-7800x3d", 599,
            "core-i7-14700k", 649,
            "ryzen-9-7900", 699
    );

    private static final Map<String, Integer> GPU_PRICES = Map.of(
            "integrated", 0,
            "arc-b580", 399,
            "rx-7800-xt", 799,
            "rtx-4060", 499,
            "rtx-5070", 999,
            "rtx-5080", 1799
    );

    private static final Map<String, Integer> MEMORY_PRICES = Map.of(
            "16gb", -120,
            "32gb", 0,
            "64gb", 160,
            "128gb", 440
    );

    private static final Map<String, Integer> STORAGE_PRICES = Map.of(
            "512gb", -160,
            "1tb", -90,
            "2tb", 0,
            "4tb", 160
    );

    private static final Map<String, Integer> MOTHERBOARD_PRICES = Map.of(
            "b650m-no-wifi", 0,
            "b650-wifi", 80,
            "b850-wifi", 180,
            "x870-wifi", 300,
            "x870e-wifi", 500,
            "b760-no-wifi", 0,
            "b760-wifi", 80,
            "z790-wifi", 260
    );

    private static final Map<String, Integer> PSU_PRICES = Map.of(
            "550-standard", -90,
            "550-bronze", -60,
            "650-bronze", 0,
            "650-silver", 40,
            "750-gold", 100,
            "850-gold", 180,
            "1000-platinum", 350,
            "1200-titanium", 600
    );

    private static final Map<String, Integer> CASE_PRICES = Map.of(
            "compact", 0,
            "mid", 80,
            "full", 180
    );

    private static final Map<String, Integer> COOLING_PRICES = Map.of(
            "tower-air", 0,
            "dual-tower-air", 80,
            "240-liquid", 150,
            "360-liquid", 240
    );

    public ConfiguratorQuoteResponse quote(ConfiguratorQuoteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Quote request must not be null");
        }

        requireKnown(request.cpuId(), CPU_PRICES, "CPU");
        requireKnown(request.gpuId(), GPU_PRICES, "GPU");
        requireKnown(request.memoryId(), MEMORY_PRICES, "memory");
        requireKnown(request.storageId(), STORAGE_PRICES, "storage");
        requireKnown(request.motherboardId(), MOTHERBOARD_PRICES, "motherboard");
        requireKnown(request.psuId(), PSU_PRICES, "PSU");
        requireKnown(request.caseId(), CASE_PRICES, "case");
        requireKnown(request.coolingId(), COOLING_PRICES, "cooling");

        requireKnown(request.recommendedCpuId(), CPU_PRICES, "recommended CPU");
        requireKnown(request.recommendedGpuId(), GPU_PRICES, "recommended GPU");
        requireKnown(request.recommendedMemoryId(), MEMORY_PRICES, "recommended memory");
        requireKnown(request.recommendedStorageId(), STORAGE_PRICES, "recommended storage");
        requireKnown(request.recommendedMotherboardId(), MOTHERBOARD_PRICES, "recommended motherboard");
        requireKnown(request.recommendedPsuId(), PSU_PRICES, "recommended PSU");
        requireKnown(request.recommendedCaseId(), CASE_PRICES, "recommended case");
        requireKnown(request.recommendedCoolingId(), COOLING_PRICES, "recommended cooling");

        int recommendedCpuPrice = CPU_PRICES.get(request.recommendedCpuId());
        int recommendedGpuPrice = GPU_PRICES.get(request.recommendedGpuId());
        int recommendedMemoryPrice = MEMORY_PRICES.get(request.recommendedMemoryId());
        int recommendedStoragePrice = STORAGE_PRICES.get(request.recommendedStorageId());
        int recommendedMotherboardPrice = MOTHERBOARD_PRICES.get(request.recommendedMotherboardId());
        int recommendedPsuPrice = PSU_PRICES.get(request.recommendedPsuId());
        int recommendedCasePrice = CASE_PRICES.get(request.recommendedCaseId());
        int recommendedCoolingPrice = COOLING_PRICES.get(request.recommendedCoolingId());

        int baseline = 899 + recommendedCpuPrice + recommendedGpuPrice;
        int selectedTotal = 899 + CPU_PRICES.get(request.cpuId()) + GPU_PRICES.get(request.gpuId())
                + (MEMORY_PRICES.get(request.memoryId()) - recommendedMemoryPrice)
                + (STORAGE_PRICES.get(request.storageId()) - recommendedStoragePrice)
                + (MOTHERBOARD_PRICES.get(request.motherboardId()) - recommendedMotherboardPrice)
                + (PSU_PRICES.get(request.psuId()) - recommendedPsuPrice)
                + (CASE_PRICES.get(request.caseId()) - recommendedCasePrice)
                + (COOLING_PRICES.get(request.coolingId()) - recommendedCoolingPrice);
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

    private void requireKnown(String id, Map<String, Integer> options, String label) {
        if (id == null || !options.containsKey(id)) throw new IllegalArgumentException("Unknown " + label + " option");
    }
}
