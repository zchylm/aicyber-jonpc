package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class ConfiguratorCompatibilityService {

    private static final List<String> AM5_MOTHERBOARDS = List.of("b650m-no-wifi", "b650-wifi", "b850-wifi", "x870-wifi", "x870e-wifi");
    private static final List<String> INTEL_MOTHERBOARDS = List.of("b760-no-wifi", "b760-wifi", "z790-wifi");
    private static final List<String> PSU_IDS = List.of("550-standard", "550-bronze", "650-bronze", "650-silver", "750-gold", "850-gold", "1000-platinum", "1200-titanium");
    private static final List<String> CASE_IDS = List.of("compact", "mid", "full");
    private static final List<String> COOLING_IDS = List.of("tower-air", "dual-tower-air", "240-liquid", "360-liquid");

    public ConfiguratorCompatibilityResponse compatibleOptions(ConfiguratorCompatibilityRequest request) {
        if (request == null || request.cpuId() == null || request.gpuId() == null) {
            throw new IllegalArgumentException("Compatibility request must include CPU and GPU");
        }

        boolean intelCpu = request.cpuId().startsWith("core-");
        List<String> motherboards = (intelCpu ? INTEL_MOTHERBOARDS : AM5_MOTHERBOARDS);
        int minimumPsu = minimumWattage(request.gpuId());
        List<String> psus = PSU_IDS.stream().filter(id -> psuWattage(id) >= minimumPsu).toList();
        boolean microAtx = "b650m-no-wifi".equals(request.motherboardId());
        List<String> cases = CASE_IDS.stream().filter(id -> !"compact".equals(id) || microAtx).toList();
        boolean highHeatCpu = List.of("ryzen-9-7900", "core-i7-14700k").contains(request.cpuId());
        List<String> cooling = COOLING_IDS.stream()
                .filter(id -> !(highHeatCpu && "tower-air".equals(id)))
                .filter(id -> !"compact".equals(request.caseId()) || "tower-air".equals(id))
                .filter(id -> !"mid".equals(request.caseId()) || !"360-liquid".equals(id))
                .toList();

        List<String> validation = Stream.of(
                        motherboards.contains(request.motherboardId()) ? null : "Selected motherboard does not match the CPU platform.",
                        psus.contains(request.psuId()) ? null : "Selected PSU wattage is below the GPU power requirement.",
                        cases.contains(request.caseId()) ? null : "Compact case requires the Micro-ATX motherboard option.",
                        cooling.contains(request.coolingId()) ? null : "Selected cooling option is not supported by the CPU and case combination."
                )
                .filter(Objects::nonNull)
                .toList();

        return new ConfiguratorCompatibilityResponse(motherboards, psus, cases, cooling, validation);
    }

    private int minimumWattage(String gpuId) {
        if ("rtx-5080".equals(gpuId)) return 850;
        if (List.of("rtx-5070", "rx-7800-xt").contains(gpuId)) return 750;
        if (List.of("rtx-4060", "arc-b580").contains(gpuId)) return 550;
        return 450;
    }

    private int psuWattage(String id) {
        return Integer.parseInt(id.substring(0, id.indexOf('-')));
    }
}
