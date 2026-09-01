package com.aicyber.backend.configurator.dto;

import java.util.List;
import java.util.Map;

public record ConfiguratorCatalogResponse(
        int systemBasePrice,
        Map<String, List<ConfiguratorCatalogOption>> options
) { }
