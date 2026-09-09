package com.aicyber.backend.order.model;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SalesOrder(
        UUID id,
        UUID userId,
        UUID buildRequestId,
        String orderReference,
        long amountCents,
        String currency,
        String status,
        OffsetDateTime paidAt,
        OffsetDateTime rewardEligibleAt
) {
}
