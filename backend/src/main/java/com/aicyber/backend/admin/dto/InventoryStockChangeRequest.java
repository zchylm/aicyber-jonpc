package com.aicyber.backend.admin.dto;

import java.util.UUID;

public record InventoryStockChangeRequest(
        UUID inventoryItemId,
        UUID locationId,
        Integer quantity,
        String reason,
        String idempotencyKey
) {
}
