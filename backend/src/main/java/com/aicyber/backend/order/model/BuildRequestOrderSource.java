package com.aicyber.backend.order.model;

import java.util.UUID;

public record BuildRequestOrderSource(
        UUID id,
        UUID userId,
        String requestReference,
        long estimatedPriceCents
) {
}
