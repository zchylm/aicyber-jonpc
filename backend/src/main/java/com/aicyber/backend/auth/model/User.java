package com.aicyber.backend.auth.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record User(
        UUID id,
        String email,
        String passwordHash,
        String displayName,
        String role,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
