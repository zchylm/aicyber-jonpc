package com.aicyber.backend.payment.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminBankTransferReviewResponse(
        UUID paymentId,
        UUID orderId,
        String paymentReference,
        String orderReference,
        String customerName,
        String customerEmail,
        long amountCents,
        String currency,
        OffsetDateTime customerReportedAt,
        OffsetDateTime expiresAt
) {}
