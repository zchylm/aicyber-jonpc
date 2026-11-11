package com.aicyber.backend.configurator.dto;

import java.util.List;

public record ConfiguratorValidationResponse(boolean compatible, List<String> validation) { }
