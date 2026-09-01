package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguratorQuoteServiceTest {

    private final ConfiguratorQuoteService service = new ConfiguratorQuoteService();

    @Test
    void recommendedSelectionsHaveNoAdjustment() {
        ConfiguratorQuoteResponse response = service.quote(new ConfiguratorQuoteRequest(
                "gaming", Map.of("budget", "$1,500–$2,000"), "ryzen-7-7700", "rtx-5070", "32gb", "2tb",
                "b850-wifi", "850-gold", "mid", "dual-tower-air", "ryzen-7-7700", "rtx-5070", "32gb", "2tb",
                "b850-wifi", "850-gold", "mid", "dual-tower-air"));

        assertEquals(0, response.selectedAdjustments());
        assertEquals(2347, response.recommendedBaseline());
        assertEquals(response.recommendedBaseline(), response.estimatedTotal());
        assertTrue(response.compatible());
    }

    @Test
    void incompatibleSelectionsAreReported() {
        ConfiguratorQuoteResponse response = service.quote(new ConfiguratorQuoteRequest(
                "gaming", Map.of("budget", "$1,500–$2,000"), "ryzen-7-7700", "rtx-5070", "32gb", "2tb",
                "b850-wifi", "850-gold", "mid", "dual-tower-air", "core-i5-14600k", "rtx-5080", "32gb", "2tb",
                "b850-wifi", "650-bronze", "compact", "tower-air"));

        assertTrue(!response.compatible());
        assertEquals(3, response.validation().size());
    }

    @Test
    void unknownCatalogIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.quote(new ConfiguratorQuoteRequest(
                "gaming", Map.of("budget", "$1,500–$2,000"), "ryzen-7-7700", "unknown-gpu", "32gb", "2tb",
                "b850-wifi", "850-gold", "mid", "dual-tower-air", "ryzen-7-7700", "unknown-gpu", "32gb", "2tb",
                "b850-wifi", "850-gold", "mid", "dual-tower-air")));
    }
}
