package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorSelection;
import com.aicyber.backend.configurator.dto.ConfiguratorValidationResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguratorValidationServiceTest {

    private final ConfiguratorValidationService service = new ConfiguratorValidationService();

    @ParameterizedTest
    @ValueSource(strings = {"cpuId", "gpuId", "memoryId", "storageId", "motherboardId", "psuId", "caseId", "coolingId",
            "recommendedCpuId", "recommendedGpuId", "recommendedMemoryId", "recommendedStorageId",
            "recommendedMotherboardId", "recommendedPsuId", "recommendedCaseId", "recommendedCoolingId"})
    void rejectsUnknownIdsInEverySelectionField(String field) {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var node = json.valueToTree(validSelection());
        ((com.fasterxml.jackson.databind.node.ObjectNode) node).put(field, "unknown-part");
        var selection = json.convertValue(node, ConfiguratorSelection.class);
        assertThrows(IllegalArgumentException.class, () -> service.validate(selection));
    }

    @ParameterizedTest
    @ValueSource(strings = {"purple", ""})
    void rejectsUnsupportedCaseFinish(String finish) {
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var node = (com.fasterxml.jackson.databind.node.ObjectNode) json.valueToTree(validSelection());
        node.put("caseColorId", finish);
        assertTrue(!service.validate(json.convertValue(node, ConfiguratorSelection.class)).compatible());
    }

    private ConfiguratorSelection validSelection() {
        return new ConfiguratorSelection("gaming", Map.of("budget", "$2,800–$3,800"),
                "ryzen-5-9600x", "rtx-5070", "32gb", "2tb", "b850-wifi", "750-gold", "mid", "dual-tower-air",
                "ryzen-5-9600x", "rtx-5070", "32gb", "2tb", "b850-wifi", "750-gold", "mid", "dual-tower-air", "black");
    }

    @Test
    void compatibleSelectionsPassWithoutPrices() {
        ConfiguratorValidationResponse response = service.validate(new ConfiguratorSelection(
                "gaming", Map.of("budget", "$2,000–$3,500"), "ryzen-5-9600x", "rtx-5070", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "ryzen-5-9600x", "rtx-5070", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "black"));

        assertEquals(java.util.List.of(), response.validation());
        assertTrue(response.compatible());
    }

    @Test
    void incompatibleSelectionsAreReported() {
        ConfiguratorValidationResponse response = service.validate(new ConfiguratorSelection(
                "gaming", Map.of("budget", "$2,000–$3,500"), "ryzen-5-9600x", "rtx-5070", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "core-ultra-7-265k", "rtx-5080", "32gb", "2tb",
                "b850-wifi", "650-bronze", "compact", "tower-air", "white"));

        assertTrue(!response.compatible());
        assertTrue(response.validation().size() >= 3);
    }

    @Test
    void unknownCatalogIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.validate(new ConfiguratorSelection(
                "gaming", Map.of("budget", "$2,000–$3,500"), "ryzen-5-9600x", "unknown-gpu", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "ryzen-5-9600x", "unknown-gpu", "32gb", "2tb",
                "b850-wifi", "750-gold", "mid", "dual-tower-air", "black")));
    }
}
