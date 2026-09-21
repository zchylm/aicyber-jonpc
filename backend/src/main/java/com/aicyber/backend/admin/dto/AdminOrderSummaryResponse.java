package com.aicyber.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminOrderSummaryResponse(
        UUID id,
        String orderReference,
        String customerName,
        String customerEmail,
        String customerReference,
        long amountCents,
        String currency,
        String status,
        String paymentStatus,
        String paymentReference,
        String invoiceNumber,
        String rewardStatus,
        OffsetDateTime createdAt
) {
}
