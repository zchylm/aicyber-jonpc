package com.aicyber.backend.catalog;

import java.util.Map;

public record SystemProductRequest(
        String sku,
        String name,
        String description,
        long priceCents,
        int plannedPreorderQuantity,
        int listingOrder,
        String productRange,
        String salesMode,
        String badge,
        String recommendedFor,
        String dispatchEstimate,
        Map<String, String> specifications,
        boolean previewEnabled
) {
}
