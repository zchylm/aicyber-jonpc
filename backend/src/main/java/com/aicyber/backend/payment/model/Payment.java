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
        String providerPaymentId,
        String providerCheckoutSessionId,
        long amountCents,
        long productSubtotalCents,
        long discountCents,
        long shippingFeeCents,
        String currency,
        String orderStatus,
        String status,
        String idempotencyKey,
        String failureReason,
        OffsetDateTime succeededAt,
        OffsetDateTime expiresAt,
        OffsetDateTime customerReportedAt,
        OffsetDateTime bankReceivedAt,
        UUID confirmedBy,
        OffsetDateTime confirmedAt,
        String reviewNote
) {
}
