package com.aicyber.backend.auth.dto;

public record ResetPasswordRequest(String token, String newPassword, String confirmPassword) {
}
