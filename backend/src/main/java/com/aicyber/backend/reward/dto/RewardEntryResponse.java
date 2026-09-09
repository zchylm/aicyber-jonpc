package com.aicyber.backend.reward.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RewardEntryResponse(
        UUID id,
        String orderReference,
        long queueSequence,
        Long currentPosition,
        long targetAmountCents,
        long allocatedAmountCents,
        long remainingAmountCents,
        double progressPercent,
        String status,
        OffsetDateTime joinedAt,
        OffsetDateTime completedAt,
        Long latestAllocationAmountCents,
        OffsetDateTime latestAllocationAt
) {
}
