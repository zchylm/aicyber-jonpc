package com.aicyber.backend.auth.dto;

public record AuthResponse(String accessToken, UserResponse user) {
}
