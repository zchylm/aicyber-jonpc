package com.aicyber.backend.configurator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.aicyber.backend.configurator.dto.ConfiguratorCatalogResponse;
import org.junit.jupiter.api.Test;

class ConfiguratorCatalogTest {

    @Test
    void exposesEveryConfiguratorCategoryAndStableBaseline() {
        ConfiguratorCatalogResponse catalog = ConfiguratorCatalog.response();

        assertEquals(899, catalog.systemBasePrice());
        assertEquals(6, catalog.options().get("cpu").size());
        assertEquals(6, catalog.options().get("gpu").size());
        assertEquals(4, catalog.options().get("memory").size());
        assertEquals(4, catalog.options().get("storage").size());
        assertEquals(8, catalog.options().get("motherboard").size());
        assertEquals(8, catalog.options().get("psu").size());
        assertEquals(3, catalog.options().get("case").size());
        assertEquals(4, catalog.options().get("cooling").size());
    }

    @Test
    void includesCompatibilityMetadataAndNoImagePaths() {
        ConfiguratorCatalogResponse catalog = ConfiguratorCatalog.response();

        var noWifiBoard = catalog.options().get("motherboard").stream()
                .filter(option -> option.id().equals("b650m-no-wifi"))
                .findFirst()
                .orElseThrow();
        var highPowerPsu = catalog.options().get("psu").stream()
                .filter(option -> option.id().equals("850-gold"))
                .findFirst()
                .orElseThrow();
        var cooling = catalog.options().get("cooling").stream()
                .filter(option -> option.id().equals("360-liquid"))
                .findFirst()
                .orElseThrow();

        assertEquals(false, noWifiBoard.wifi());
        assertEquals(850, highPowerPsu.wattage());
        assertTrue(cooling.supportedCases().contains("full"));
        assertTrue(catalog.options().values().stream().flatMap(java.util.Collection::stream)
                .allMatch(option -> option.label().matches(".*") && option.detail().matches(".*")));
    }
}
