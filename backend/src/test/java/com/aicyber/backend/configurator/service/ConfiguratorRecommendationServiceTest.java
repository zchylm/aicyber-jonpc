package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorRecommendationRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorRecommendationResponse;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfiguratorRecommendationServiceTest {

    private final ConfiguratorRecommendationService service = new ConfiguratorRecommendationService();

    @Test
    void gamingRecommendationUsesTheBudgetAwareStartingPoint() {
        ConfiguratorRecommendationResponse response = service.recommend(new ConfiguratorRecommendationRequest(
                "gaming",
                Map.of("resolution", "1440p", "games", "Mixed", "budget", "$1,500-$2,000")
        ));

        assertEquals("ryzen-7-7700", response.cpuId());
        assertEquals("arc-b580", response.gpuId());
        assertEquals("32gb", response.memoryId());
        assertEquals("2tb", response.storageId());
    }

    @Test
    void heavyAiRecommendationAddsMemoryAndStorageHeadroom() {
        ConfiguratorRecommendationResponse response = service.recommend(new ConfiguratorRecommendationRequest(
                "ai",
                Map.of("workload", "Local LLM", "scale", "Heavy", "budget", "$3,000-$5,000")
        ));

        assertEquals("rtx-5080", response.gpuId());
        assertEquals("ryzen-5-7600", response.cpuId());
        assertEquals("128gb", response.memoryId());
        assertEquals("4tb", response.storageId());
        assertEquals("b850-wifi", response.motherboardId());
        assertEquals("850-gold", response.psuId());
    }
}
