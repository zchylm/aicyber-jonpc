package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorCompatibilityResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguratorCompatibilityServiceTest {

    private final ConfiguratorCompatibilityService service = new ConfiguratorCompatibilityService();

    @Test
    void highPowerGpuFiltersOutUndersizedPowerSupplies() {
        ConfiguratorCompatibilityResponse response = service.compatibleOptions(new ConfiguratorCompatibilityRequest(
                "ryzen-7-9700x", "rtx-5080", "b850-wifi", "850-gold", "mid", "dual-tower-air"
        ));

        assertFalse(response.psuIds().contains("750-gold"));
        assertTrue(response.psuIds().contains("850-gold"));
        assertTrue(response.validation().isEmpty());
    }

    @Test
    void intelCpuOnlyExposesIntelPlatformBoards() {
        ConfiguratorCompatibilityResponse response = service.compatibleOptions(new ConfiguratorCompatibilityRequest(
                "core-ultra-5-245k", "rtx-5060", "b860-wifi", "650-bronze", "mid", "tower-air"
        ));

        assertTrue(response.motherboardIds().contains("b860-wifi"));
        assertFalse(response.motherboardIds().contains("b850-wifi"));
        assertTrue(response.validation().isEmpty());
    }

    @Test
    void flagshipGraphicsCannotUseCompactCase() {
        ConfiguratorCompatibilityResponse response = service.compatibleOptions(new ConfiguratorCompatibilityRequest(
                "ryzen-7-9700x", "rtx-5080", "b850m-wifi", "850-gold", "compact", "240-liquid"
        ));

        assertFalse(response.caseIds().contains("compact"));
        assertFalse(response.validation().isEmpty());
    }

    @Test
    void cpuWithoutIntegratedGraphicsIsRejectedForIntegratedSelection() {
        ConfiguratorCompatibilityResponse response = service.compatibleOptions(new ConfiguratorCompatibilityRequest(
                "ryzen-5-7500f", "integrated", "b850m-wifi", "650-bronze", "compact", "tower-air"
        ));

        assertTrue(response.validation().stream().anyMatch(message -> message.contains("integrated graphics")));
    }
}
