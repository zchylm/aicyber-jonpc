package com.aicyber.backend.catalog;

import java.util.Map;
import java.util.UUID;

public record PublicSystemPreview(
        UUID id,
        String sku,
        String name,
        String description,
        long priceCents,
        boolean available,
        String badge,
        String recommendedFor,
        String dispatchEstimate,
        Map<String, String> specifications
) {
    public static PublicSystemPreview from(SystemProduct product) {
        Map<String, String> specs = new java.util.LinkedHashMap<>(product.specifications());
        specs.remove("Warranty");
        return new PublicSystemPreview(product.id(), product.sku(), product.name(), product.description(),
                product.priceCents(), product.availableQuantity() > 0, product.badge(), product.recommendedFor(), product.dispatchEstimate(), specs);
    }
}
