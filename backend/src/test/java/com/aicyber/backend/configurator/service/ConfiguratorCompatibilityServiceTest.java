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
                "ryzen-7-7700", "rtx-5080", "b850-wifi", "850-gold", "mid", "dual-tower-air"
        ));

        assertFalse(response.psuIds().contains("750-gold"));
        assertTrue(response.psuIds().contains("850-gold"));
        assertTrue(response.validation().isEmpty());
    }

    @Test
    void intelCpuOnlyExposesIntelPlatformBoards() {
        ConfiguratorCompatibilityResponse response = service.compatibleOptions(new ConfiguratorCompatibilityRequest(
                "core-i5-14600k", "rtx-4060", "b760-wifi", "650-bronze", "mid", "tower-air"
        ));

        assertTrue(response.motherboardIds().contains("b760-wifi"));
        assertFalse(response.motherboardIds().contains("b850-wifi"));
        assertTrue(response.validation().isEmpty());
    }
}
