package com.aicyber.backend.configurator.dto;

public record ConfiguratorBuildRequest(
        String name,
        String email,
        String phone,
        String location,
        String notes,
        boolean contact,
        ConfiguratorQuoteRequest configuration
) { }
