package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorBuildRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorBuildResponse;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguratorRequestServiceTest {

    private final ConfiguratorRequestService service = new ConfiguratorRequestService(new ConfiguratorQuoteService(), (userId, request, quote, reference) -> { });

    @Test
    void acceptsCompatibleBuildAndRecalculatesServerPrice() {
        ConfiguratorBuildResponse response = service.submit(new ConfiguratorBuildRequest(
                "Jambo", "jambo@example.com", "0400000000", "Melbourne", "", true,
                quote("gaming", "ryzen-7-7700", "arc-b580", "32gb", "2tb", "b850-wifi", "650-bronze", "mid", "dual-tower-air")));

        assertEquals("RECEIVED", response.status());
        assertTrue(response.requestReference().matches("JON-GAM-[A-Z0-9]{6}"));
        assertEquals(1747, response.quote().estimatedTotal());
    }

    @Test
    void createsShortReferenceForAiDirection() {
        ConfiguratorBuildResponse response = service.submit(new ConfiguratorBuildRequest(
                "Jambo", "ai@example.com", "0400000000", "Melbourne", "", true,
                quote("ai", "ryzen-7-7700", "rtx-4060", "32gb", "2tb", "b850-wifi", "650-bronze", "mid", "dual-tower-air")));

        assertTrue(response.requestReference().matches("JON-AI-[A-Z0-9]{6}"));
    }

    @Test
    void rejectsIncompatibleBuildBeforeItIsReceived() {
        assertThrows(IllegalArgumentException.class, () -> service.submit(new ConfiguratorBuildRequest(
                "Jambo", "jambo@example.com", "", "Melbourne", "", true,
                quote("gaming", "ryzen-7-7700", "rtx-5080", "32gb", "2tb", "b850-wifi", "650-bronze", "mid", "dual-tower-air"))));
    }

    @Test
    void rejectsInvalidContactDetails() {
        assertThrows(IllegalArgumentException.class, () -> service.submit(new ConfiguratorBuildRequest(
                "", "not-an-email", "", "", "", false,
                quote("gaming", "ryzen-7-7700", "arc-b580", "32gb", "2tb", "b850-wifi", "650-bronze", "mid", "dual-tower-air"))));
    }

    private ConfiguratorQuoteRequest quote(String direction, String cpu, String gpu, String memory, String storage, String motherboard, String psu, String caseId, String cooling) {
        return new ConfiguratorQuoteRequest(direction, Map.of("budget", "$1,500–$2,000"),
                "ryzen-7-7700", "arc-b580", "32gb", "2tb", "b850-wifi", "650-bronze", "mid", "dual-tower-air",
                cpu, gpu, memory, storage, motherboard, psu, caseId, cooling);
    }
}
