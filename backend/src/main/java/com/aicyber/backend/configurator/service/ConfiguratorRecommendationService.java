package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorRecommendationRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorRecommendationResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ConfiguratorRecommendationService {

    private static final List<String> CPU_IDS = List.of(
            "ryzen-5-7600", "core-i5-14600k", "ryzen-7-7700", "ryzen-7-7800x3d", "core-i7-14700k", "ryzen-9-7900"
    );
    private static final List<String> GPU_IDS = List.of(
            "integrated", "arc-b580", "rx-7800-xt", "rtx-4060", "rtx-5070", "rtx-5080"
    );

    public ConfiguratorRecommendationResponse recommend(ConfiguratorRecommendationRequest request) {
        if (request == null || request.direction() == null) {
            throw new IllegalArgumentException("Recommendation request must include a direction");
        }
        Map<String, String> answers = request.answers() == null ? Map.of() : request.answers();
        String resolution = answers.getOrDefault("resolution", "");
        String workload = firstAnswer(answers, "workload", "creativeWork", "use");
        String scale = firstAnswer(answers, "scale", "reliability", "priority");

        String initialGpu;
        String initialCpu;
        switch (request.direction()) {
            case "gaming" -> {
                initialGpu = "1080p".equals(resolution) ? "rtx-4060" : "4K".equals(resolution) ? "rtx-5080" : "rtx-5070";
                initialCpu = "1080p".equals(resolution) ? "ryzen-5-7600" : "Competitive".equals(answers.get("games")) ? "ryzen-7-7800x3d" : "ryzen-7-7700";
            }
            case "ai" -> {
                initialGpu = "Light".equals(scale) ? "rtx-4060" : "Heavy".equals(scale) ? "rtx-5080" : "rtx-5070";
                initialCpu = "Heavy".equals(scale) ? "ryzen-9-7900" : "ryzen-7-7700";
            }
            case "creator" -> {
                initialGpu = "1080p".equals(resolution) ? "rtx-4060" : "8K".equals(resolution) ? "rtx-5080" : "rtx-5070";
                initialCpu = "8K".equals(resolution) || "3D and motion".equals(workload) ? "ryzen-9-7900" : "ryzen-7-7700";
            }
            case "workstation" -> {
                initialGpu = "Development".equals(workload) && "Quiet operation".equals(scale) ? "rtx-4060" : "rtx-5070";
                initialCpu = List.of("Simulation", "Data processing").contains(workload) ? "ryzen-9-7900" : "core-i7-14700k";
            }
            case "enterprise" -> {
                initialGpu = "Office productivity".equals(workload) ? "integrated" : "rtx-4060";
                initialCpu = "Office productivity".equals(workload) ? "ryzen-5-7600" : "core-i5-14600k";
            }
            default -> throw new IllegalArgumentException("Unknown direction: " + request.direction());
        }

        int[] budget = parseBudget(answers.getOrDefault("budget", ""));
        String bestCpu = initialCpu;
        String bestGpu = initialGpu;
        double bestScore = Double.MAX_VALUE;
        for (String cpu : CPU_IDS) {
            for (String gpu : GPU_IDS) {
                int price = ConfiguratorCatalog.SYSTEM_BASE_PRICE + ConfiguratorCatalog.CPU_PRICES.get(cpu) + ConfiguratorCatalog.GPU_PRICES.get(gpu);
                int outsideDistance = price < budget[0] ? budget[0] - price : price > budget[1] ? price - budget[1] : 0;
                double score = outsideDistance * (price > budget[1] ? 4.0 : 0.7);
                if (price >= budget[0] && price <= budget[1]) score += Math.abs(price - (budget[0] + budget[1]) / 2.0) * 0.25;
                score += preferencePenalty(request.direction(), answers, cpu, gpu);
                if (cpu.equals(initialCpu) && gpu.equals(initialGpu)) score -= 50;
                if (score < bestScore) {
                    bestScore = score;
                    bestCpu = cpu;
                    bestGpu = gpu;
                }
            }
        }

        String memory = recommendMemory(request.direction(), answers);
        String storage = recommendStorage(request.direction(), answers);
        String motherboard = bestCpu.startsWith("core-") ? "b760-wifi" : "b850-wifi";
        String psu = minimumWattage(bestGpu) <= 550 ? "650-bronze" : (minimumWattage(bestGpu) + 100) + "-gold";
        if (!List.of("550-standard", "550-bronze", "650-bronze", "650-silver", "750-gold", "850-gold", "1000-platinum", "1200-titanium").contains(psu)) {
            psu = "850-gold";
        }
        String caseId = "b650m-no-wifi".equals(motherboard) ? "compact" : "mid";
        String cooling = List.of("ryzen-9-7900", "core-i7-14700k").contains(bestCpu) ? "240-liquid" : "dual-tower-air";

        return new ConfiguratorRecommendationResponse(bestCpu, bestGpu, memory, storage, motherboard, psu, caseId, cooling);
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

    private double preferencePenalty(String direction, Map<String, String> answers, String cpu, String gpu) {
        double score = 0;
        String resolution = answers.getOrDefault("resolution", "");
        String workload = firstAnswer(answers, "workload", "creativeWork", "use");
        if ("gaming".equals(direction)) {
            if ("integrated".equals(gpu)) score += 800;
            if ("4K".equals(resolution) && !"rtx-5080".equals(gpu)) score += 500;
            if ("1440p".equals(resolution) && "integrated".equals(gpu)) score += 300;
            if ("Competitive".equals(answers.get("games")) && !"ryzen-7-7800x3d".equals(cpu)) score += 180;
        }
        if ("ai".equals(direction) && !gpu.startsWith("rtx-")) score += 400;
        if (("creator".equals(direction) || "workstation".equals(direction)) && "ryzen-5-7600".equals(cpu)) score += 150;
        if (List.of("Simulation", "Data processing", "3D and motion").contains(workload) && "ryzen-5-7600".equals(cpu)) score += 250;
        if ("enterprise".equals(direction) && "Office productivity".equals(workload) && !"integrated".equals(gpu)) score += 160;
        return score;
    }

    private int minimumWattage(String gpu) {
        if ("rtx-5080".equals(gpu)) return 850;
        if (List.of("rtx-5070", "rx-7800-xt").contains(gpu)) return 750;
        if (List.of("rtx-4060", "arc-b580").contains(gpu)) return 550;
        return 450;
    }

    private String firstAnswer(Map<String, String> answers, String... keys) {
        for (String key : keys) if (answers.containsKey(key)) return answers.get(key);
        return "";
    }

    private int[] parseBudget(String value) {
        String[] values = value.replace("$", "").replace(",", "").split("[–-]");
        if (values.length == 2) {
            try { return new int[]{Integer.parseInt(values[0]), Integer.parseInt(values[1])}; }
            catch (NumberFormatException ignored) { }
        }
        return new int[]{0, Integer.MAX_VALUE};
    }
}
