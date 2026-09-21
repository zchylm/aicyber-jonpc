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
                "gaming", Map.of("budget", "$2,000–$3,500"), "ryzen-5-9600x", "rtx-5070", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "ryzen-5-9600x", "rtx-5070", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "black"));

        assertEquals(0, response.selectedAdjustments());
        assertEquals(3397, response.recommendedBaseline());
        assertEquals(response.recommendedBaseline(), response.estimatedTotal());
        assertTrue(response.compatible());
    }

    @Test
    void incompatibleSelectionsAreReported() {
        ConfiguratorQuoteResponse response = service.quote(new ConfiguratorQuoteRequest(
                "gaming", Map.of("budget", "$2,000–$3,500"), "ryzen-5-9600x", "rtx-5070", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "core-ultra-7-265k", "rtx-5080", "32gb", "2tb",
                "b850-wifi", "650-bronze", "compact", "tower-air", "white"));

        assertTrue(!response.compatible());
        assertTrue(response.validation().size() >= 3);
    }

    @Test
    void unknownCatalogIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.quote(new ConfiguratorQuoteRequest(
                "gaming", Map.of("budget", "$2,000–$3,500"), "ryzen-5-9600x", "unknown-gpu", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "ryzen-5-9600x", "unknown-gpu", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "black")));
    }
}
