package com.aicyber.backend.catalog;

import java.util.Map;
import java.util.UUID;

public record SystemProduct(
        UUID id,
        String sku,
        String name,
        String description,
        long priceCents,
        int plannedPreorderQuantity,
        int onHandQuantity,
        int reorderPoint,
        int availableQuantity,
        int listingOrder,
        String productRange,
        String salesMode,
        String sourceRevision,
        String badge,
        String recommendedFor,
        String dispatchEstimate,
        Map<String, String> specifications,
        String status,
        boolean previewEnabled
) {
}
