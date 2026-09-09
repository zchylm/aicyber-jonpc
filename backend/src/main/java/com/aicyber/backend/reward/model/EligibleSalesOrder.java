package com.aicyber.backend.reward.model;

import java.util.UUID;

public record EligibleSalesOrder(
        UUID id,
        long amountCents,
        String currency,
        String status
) {
}
