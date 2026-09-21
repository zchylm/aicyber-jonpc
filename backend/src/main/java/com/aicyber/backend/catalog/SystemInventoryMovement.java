package com.aicyber.backend.catalog;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SystemInventoryMovement(
        UUID id,
        UUID systemBuildId,
        String movementType,
        int quantityDelta,
        int quantityAfter,
        String reason,
        UUID performedBy,
        String idempotencyKey,
        OffsetDateTime createdAt
) {
}
