package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class ConfiguratorCompatibilityService {

    public ConfiguratorCompatibilityResponse compatibleOptions(ConfiguratorCompatibilityRequest request) {
        if (request == null || !ConfiguratorCatalog.CPU_PRICES.containsKey(request.cpuId())
                || !ConfiguratorCatalog.GPU_PRICES.containsKey(request.gpuId())) {
            throw new IllegalArgumentException("Compatibility request must include a known CPU and GPU");
        }

        List<String> motherboards = "LGA1851".equals(ConfiguratorCatalog.platformForCpu(request.cpuId()))
                ? ConfiguratorCatalog.LGA1851_MOTHERBOARDS : ConfiguratorCatalog.AM5_MOTHERBOARDS;
        List<String> psus = ConfiguratorCatalog.PSU_PRICES.keySet().stream()
                .filter(id -> psuWattage(id) >= ConfiguratorCatalog.minimumPsuWattage(request.gpuId()))
                .toList();
        boolean microAtx = "b850m-wifi".equals(request.motherboardId());
        List<String> cases = ConfiguratorCatalog.CASE_PRICES.keySet().stream()
                .filter(id -> !"compact".equals(id) || microAtx)
                .filter(id -> !("compact".equals(id) && List.of("rtx-5080", "rtx-5090").contains(request.gpuId())))
                .toList();
        List<String> cooling = ConfiguratorCatalog.COOLING_PRICES.keySet().stream()
                .filter(id -> !ConfiguratorCatalog.highHeatCpu(request.cpuId()) || !"tower-air".equals(id))
                .filter(id -> !"compact".equals(request.caseId()) || List.of("tower-air", "240-liquid").contains(id))
                .filter(id -> !"mid".equals(request.caseId()) || !"360-liquid".equals(id) || !"rtx-5090".equals(request.gpuId()))
                .toList();

        List<String> validation = Stream.of(
                        motherboards.contains(request.motherboardId()) ? null : "Selected motherboard does not match the CPU platform.",
                        psus.contains(request.psuId()) ? null : "Selected PSU wattage is below the GPU power requirement.",
                        cases.contains(request.caseId()) ? null : "Selected case does not support this motherboard and graphics combination.",
                        cooling.contains(request.coolingId()) ? null : "Selected cooling option is not supported by the CPU and case combination.",
                        "integrated".equals(request.gpuId()) && !ConfiguratorCatalog.hasIntegratedGraphics(request.cpuId())
                                ? "The selected CPU does not include integrated graphics." : null
                )
                .filter(Objects::nonNull)
                .toList();

        return new ConfiguratorCompatibilityResponse(motherboards, psus, cases, cooling, validation);
    }

    private int psuWattage(String id) {
        return Integer.parseInt(id.substring(0, id.indexOf('-')));
    }
}
