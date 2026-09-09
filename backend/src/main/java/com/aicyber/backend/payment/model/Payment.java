package com.aicyber.backend.payment.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID orderId,
        String orderReference,
        UUID userId,
        String paymentReference,
        String provider,
        long amountCents,
        String currency,
        String orderStatus,
        String status,
        String idempotencyKey,
        String failureReason,
        OffsetDateTime succeededAt
) {
}
