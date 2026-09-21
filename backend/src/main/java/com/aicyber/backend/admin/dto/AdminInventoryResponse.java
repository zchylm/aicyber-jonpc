package com.aicyber.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminInventoryResponse(
        Summary summary,
        List<Category> categories,
        List<Location> locations,
        List<Item> items,
        List<Movement> recentMovements
) {
    public record Summary(
            long totalSkus,
            long totalOnHand,
            long totalReserved,
            long lowStockSkus,
            long inventoryCostCents
    ) {
    }

    public record Category(UUID id, String code, String name) {
    }

    public record Location(
            UUID id,
            String code,
            String name,
            String locationType,
            String status,
            long onHandQuantity,
            long reservedQuantity
    ) {
    }

    public record Item(
            UUID id,
            String categoryCode,
            String categoryName,
            String sku,
            String brand,
            String model,
            String displayName,
            String description,
            Map<String, Object> specifications,
            Long unitCostCents,
            long retailPriceCents,
            String currency,
            String status,
            long onHandQuantity,
            long reservedQuantity,
            long availableQuantity,
            OffsetDateTime updatedAt
    ) {
    }

    public record Movement(
            UUID id,
            String sku,
            String itemName,
            String locationCode,
            String movementType,
            int onHandDelta,
            int reservedDelta,
            int onHandAfter,
            int reservedAfter,
            String reason,
            String performedBy,
            OffsetDateTime createdAt
    ) {
    }
}
