package com.aicyber.backend.admin.dto;

import java.util.Map;

public record InventoryItemRequest(
        String categoryCode,
        String sku,
        String brand,
        String model,
        String displayName,
        String description,
        Map<String, Object> specifications,
        Long unitCostCents,
        Long retailPriceCents,
        String currency,
        String status
) {
}
