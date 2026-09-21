package com.aicyber.backend.reward.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RewardEntryResponse(
        UUID id,
        String orderReference,
        long founderNumber,
        String tierName,
        int rateBasisPoints,
        long capCents,
        long purchaseAmountCents,
        long cashbackAmountCents,
        long effectivePriceCents,
        String status,
        OffsetDateTime lockedAt,
        OffsetDateTime payableAt,
        OffsetDateTime payoutDueAt,
        OffsetDateTime processingAt,
        OffsetDateTime paidAt,
        String payoutMethod,
        String payoutReference,
        String payoutFailureReason
) {
}
