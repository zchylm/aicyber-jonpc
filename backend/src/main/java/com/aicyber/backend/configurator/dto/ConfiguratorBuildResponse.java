package com.aicyber.backend.configurator.dto;

public record ConfiguratorBuildResponse(
        String requestReference,
        String status,
        ConfiguratorQuoteResponse quote,
        String message
) { }
