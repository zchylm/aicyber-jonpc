package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorRecommendationRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorRecommendationResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConfiguratorRecommendationService {

    public ConfiguratorRecommendationResponse recommend(ConfiguratorRecommendationRequest request) {
        if (request == null || request.direction() == null) {
            throw new IllegalArgumentException("Recommendation request must include a direction");
        }
        Map<String, String> answers = request.answers() == null ? Map.of() : request.answers();
        String resolution = answers.getOrDefault("resolution", "");
        String workload = firstAnswer(answers, "workload", "creativeWork", "use");
        String scale = firstAnswer(answers, "scale", "reliability", "priority");

        String cpu;
        String gpu;
        switch (request.direction()) {
            case "gaming" -> {
                cpu = "Competitive".equals(answers.get("games")) ? "ryzen-7-9850x3d"
                        : "4K".equals(resolution) ? "ryzen-7-9700x" : "ryzen-5-9600x";
                gpu = "1080p".equals(resolution) ? "rtx-5060" : "4K".equals(resolution) ? "rtx-5080" : "rtx-5070";
            }
            case "ai" -> {
                cpu = "Heavy".equals(scale) ? "ryzen-9-9950x3d" : "ryzen-9-9900x";
                gpu = "Light".equals(scale) ? "rtx-5060-ti-16gb" : "Heavy".equals(scale) ? "rtx-5080" : "rtx-5070-ti";
            }
            case "creator" -> {
                boolean demanding = "8K".equals(resolution) || "3D and motion".equals(workload);
                cpu = demanding ? "ryzen-9-9900x" : "ryzen-7-9700x";
                gpu = demanding ? "rtx-5080" : "1080p".equals(resolution) ? "rtx-5060-ti-16gb" : "rtx-5070";
            }
            case "workstation" -> {
                boolean computeHeavy = List.of("Simulation", "Data processing").contains(workload);
                cpu = computeHeavy ? "ryzen-9-9900x" : "core-ultra-7-265k";
                gpu = computeHeavy ? "rtx-5070-ti" : "rtx-5060-ti-16gb";
            }
            case "enterprise" -> {
                boolean office = "Office productivity".equals(workload);
                cpu = office ? "core-ultra-5-245k" : "core-ultra-7-265k";
                gpu = office ? "integrated" : "rtx-5060";
            }
            default -> throw new IllegalArgumentException("Unknown direction: " + request.direction());
        }

        String memory = recommendMemory(request.direction(), answers);
        String storage = recommendStorage(request.direction(), answers);
        String motherboard = recommendMotherboard(cpu);
        String psu = recommendPsu(gpu);
        String caseId = "b850m-wifi".equals(motherboard) ? "compact" : "mid";
        String cooling = recommendCooling(cpu, caseId);

        return new ConfiguratorRecommendationResponse(cpu, gpu, memory, storage, motherboard, psu, caseId, cooling);
    }

    private String recommendMemory(String direction, Map<String, String> answers) {
        String workload = firstAnswer(answers, "workload", "creativeWork", "use");
        String scale = firstAnswer(answers, "scale", "reliability", "priority");
        if ("ai".equals(direction) && "Heavy".equals(scale)) return "128gb";
        if ("workstation".equals(direction) && List.of("Data processing", "Simulation").contains(workload)) return "64gb";
        if ("creator".equals(direction) && ("3D and motion".equals(workload) || "8K".equals(answers.get("resolution")))) return "64gb";
        if ("enterprise".equals(direction) && "Office productivity".equals(workload)) return "16gb";
        return "32gb";
    }

    private String recommendStorage(String direction, Map<String, String> answers) {
        String workload = firstAnswer(answers, "workload", "creativeWork", "use");
        String scale = firstAnswer(answers, "scale", "reliability", "priority");
        if ("enterprise".equals(direction) && "Office productivity".equals(workload)) return "1tb";
        if ("ai".equals(direction) && "Heavy".equals(scale)) return "4tb";
        if ("workstation".equals(direction) && List.of("Data processing", "Simulation").contains(workload)) return "4tb";
        return "2tb";
    }

    private String recommendMotherboard(String cpu) {
        if (cpu.startsWith("core-ultra-")) return "core-ultra-7-265k".equals(cpu) ? "z890-wifi" : "b860-wifi";
        if (List.of("ryzen-9-9900x", "ryzen-9-9950x3d").contains(cpu)) return "x870-wifi";
        if ("ryzen-5-9600x".equals(cpu)) return "b850m-wifi";
        return "b850-wifi";
    }

    private String recommendPsu(String gpu) {
        int minimum = ConfiguratorCatalog.minimumPsuWattage(gpu);
        return List.of("650-bronze", "750-gold", "850-gold", "1000-platinum").stream()
                .filter(id -> Integer.parseInt(id.substring(0, id.indexOf('-'))) >= minimum)
                .findFirst()
                .orElse("1000-platinum");
    }

    private String recommendCooling(String cpu, String caseId) {
        if ("ryzen-9-9950x3d".equals(cpu)) return "360-liquid";
        if (ConfiguratorCatalog.highHeatCpu(cpu)) return "240-liquid";
        return "compact".equals(caseId) ? "tower-air" : "dual-tower-air";
    }

    private String firstAnswer(Map<String, String> answers, String... keys) {
        for (String key : keys) if (answers.containsKey(key)) return answers.get(key);
        return "";
    }
}
