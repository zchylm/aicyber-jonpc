package com.aicyber.backend.ai.dto;

import java.util.List;

public record ChatResponse(String title, String body, List<String> bullets, String source) {
}
