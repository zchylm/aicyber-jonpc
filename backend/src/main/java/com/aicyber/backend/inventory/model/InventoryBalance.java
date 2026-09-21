package com.aicyber.backend.inventory.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InventoryBalance(
        UUID inventoryItemId,
        UUID locationId,
        int onHandQuantity,
        int reservedQuantity,
        int reorderPoint,
        long version,
        OffsetDateTime updatedAt
) {
    public int availableQuantity() {
        return onHandQuantity - reservedQuantity;
    }
}
