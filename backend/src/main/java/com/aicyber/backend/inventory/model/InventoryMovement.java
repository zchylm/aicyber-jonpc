package com.aicyber.backend.inventory.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryMovement(
        UUID id,
        UUID inventoryItemId,
        UUID locationId,
        String movementType,
        int onHandDelta,
        int reservedDelta,
        int onHandAfter,
        int reservedAfter,
        UUID salesOrderId,
        UUID reservationId,
        String reason,
        UUID performedBy,
        String idempotencyKey,
        OffsetDateTime createdAt
) {
}
