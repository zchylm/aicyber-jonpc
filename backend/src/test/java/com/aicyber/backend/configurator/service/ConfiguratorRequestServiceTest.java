package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorBuildRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorBuildResponse;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.email.service.CustomerEmailService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConfiguratorRequestServiceTest {

    private final CustomerEmailService customerEmails = mock(CustomerEmailService.class);
    private final ConfiguratorRequestService service = new ConfiguratorRequestService(
            new ConfiguratorQuoteService(), (userId, request, quote, reference) -> java.util.UUID.randomUUID(), customerEmails);

    @Test
    void acceptsCompatibleBuildAndRecalculatesServerPrice() {
        ConfiguratorBuildResponse response = service.submit(new ConfiguratorBuildRequest(
                "Jambo", "jambo@example.com", "0400000000", "Melbourne", "", true,
                quote("gaming", "ryzen-5-9600x", "rtx-5060", "32gb", "2tb", "b850m-wifi", "650-bronze", "compact", "tower-air")));

        assertEquals("RECEIVED", response.status());
        assertTrue(response.requestReference().matches("JON-GAM-[A-Z0-9]{6}"));
        assertEquals(2597, response.quote().estimatedTotal());
    }

    @Test
    void createsShortReferenceForAiDirection() {
        ConfiguratorBuildResponse response = service.submit(new ConfiguratorBuildRequest(
                "Jambo", "ai@example.com", "0400000000", "Melbourne", "", true,
                quote("ai", "ryzen-9-9900x", "rtx-5070", "64gb", "2tb", "x870-wifi", "750-gold", "mid", "240-liquid")));

        assertTrue(response.requestReference().matches("JON-AI-[A-Z0-9]{6}"));
    }

    @Test
    void passesAuthenticatedUserIdToTheRequestStore() {
        BuildRequestStore requestStore = mock(BuildRequestStore.class);
        ConfiguratorRequestService authenticatedService = new ConfiguratorRequestService(
                new ConfiguratorQuoteService(), requestStore, customerEmails);
        java.util.UUID userId = java.util.UUID.randomUUID();
        java.util.UUID requestId = java.util.UUID.randomUUID();
        org.mockito.Mockito.when(requestStore.create(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(requestId);

        authenticatedService.submit(new ConfiguratorBuildRequest(
                "Jambo", "owner@example.com", "0400000000", "Melbourne", "", true,
                quote("gaming", "ryzen-5-9600x", "rtx-5060", "32gb", "2tb", "b850m-wifi", "650-bronze", "compact", "tower-air")), userId);

        verify(requestStore).create(org.mockito.ArgumentMatchers.eq(userId), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
        verify(customerEmails).buildReceived(org.mockito.ArgumentMatchers.eq(requestId),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void rejectsIncompatibleBuildBeforeItIsReceived() {
        assertThrows(IllegalArgumentException.class, () -> service.submit(new ConfiguratorBuildRequest(
                "Jambo", "jambo@example.com", "", "Melbourne", "", true,
                quote("gaming", "ryzen-7-9700x", "rtx-5080", "32gb", "2tb", "b850-wifi", "650-bronze", "mid", "dual-tower-air"))));
    }

    @Test
    void rejectsInvalidContactDetails() {
        assertThrows(IllegalArgumentException.class, () -> service.submit(new ConfiguratorBuildRequest(
                "", "not-an-email", "", "", "", false,
                quote("gaming", "ryzen-5-9600x", "rtx-5060", "32gb", "2tb", "b850m-wifi", "650-bronze", "compact", "tower-air"))));
    }

    private ConfiguratorQuoteRequest quote(String direction, String cpu, String gpu, String memory, String storage, String motherboard, String psu, String caseId, String cooling) {
        return new ConfiguratorQuoteRequest(direction, Map.of("budget", "$1,500–$2,000"),
                "ryzen-5-9600x", "rtx-5060", "32gb", "2tb", "b850m-wifi", "650-bronze", "compact", "tower-air",
                cpu, gpu, memory, storage, motherboard, psu, caseId, cooling, "black");
    }
}
