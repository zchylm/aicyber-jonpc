package com.aicyber.backend.build.dto;

import com.aicyber.backend.configurator.dto.ConfiguratorSelection;

public record SaveBuildRequest(
        String name,
        String direction,
        String budgetRange,
        ConfiguratorSelection configuration
) { }
