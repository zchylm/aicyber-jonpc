package com.aicyber.backend.auth.dto;

public record RegisterRequest(String email, String password, String displayName) {
}
