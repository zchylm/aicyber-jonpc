package com.aicyber.backend.auth.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountActionToken(
        UUID id,
        UUID userId,
        String purpose,
        OffsetDateTime expiresAt
) {
}
