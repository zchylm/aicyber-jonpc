package com.aicyber.backend.configurator.service;

import com.aicyber.backend.configurator.dto.ConfiguratorBuildRequest;

import java.util.UUID;

public interface BuildRequestStore {
    UUID create(UUID userId, ConfiguratorBuildRequest request, String reference);
}
