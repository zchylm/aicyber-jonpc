package com.aicyber.backend.order.dto;

import java.util.UUID;

public record OrderSystemItemResponse(
        UUID systemId,
        String sku,
        String name,
        long unitPriceCents,
        int quantity,
        long lineTotalCents
) {}
