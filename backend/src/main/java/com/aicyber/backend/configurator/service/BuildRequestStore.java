package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorBuildRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteResponse;

import java.util.UUID;

public interface BuildRequestStore {
    void create(UUID userId, ConfiguratorBuildRequest request, ConfiguratorQuoteResponse quote, String reference);
}
