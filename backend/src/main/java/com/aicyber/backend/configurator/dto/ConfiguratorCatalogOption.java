package com.aicyber.backend.configurator.dto;

import java.util.List;

/** Structured hardware data returned to the configurator UI. Image assets remain frontend-owned. */
public record ConfiguratorCatalogOption(
        String id,
        String label,
        String family,
        String detail,
        int price,
        String platform,
        String chipset,
        Boolean wifi,
        String formFactor,
        String efficiency,
        Integer wattage,
        String modular,
        String recommendedFor,
        List<String> supportedCases,
        List<String> formFactors
) { }
