package com.aicyber.backend.ai.dto;

import java.util.List;

public record ChatRequest(String message, List<ChatTurn> history) {
}
