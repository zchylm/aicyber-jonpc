package com.aicyber.backend.order.dto;

import java.time.OffsetDateTime;

public record OrderFulfilmentResponse(
        String status,
        String carrier,
        String trackingNumber,
        OffsetDateTime dispatchedAt,
        OffsetDateTime deliveredAt,
        OffsetDateTime autoConfirmAt,
        OffsetDateTime receivedAt,
        OffsetDateTime issueReportedAt,
        String issueSummary
) {
}
