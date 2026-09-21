package com.aicyber.backend.catalog;

public record SystemStockChangeRequest(
        Integer quantity,
        String reason,
        String idempotencyKey
) {
}
