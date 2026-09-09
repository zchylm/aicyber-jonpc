package com.aicyber.backend.reward.model;

import java.util.Objects;
import java.util.UUID;

public record QueueEntryBalance(
        UUID queueEntryId,
        long queueSequence,
        long targetAmountCents,
        long allocatedAmountCents
) {
    public QueueEntryBalance {
        Objects.requireNonNull(queueEntryId, "queueEntryId is required");
        if (queueSequence <= 0) {
            throw new IllegalArgumentException("queueSequence must be positive");
        }
        if (targetAmountCents <= 0) {
            throw new IllegalArgumentException("targetAmountCents must be positive");
        }
        if (allocatedAmountCents < 0 || allocatedAmountCents > targetAmountCents) {
            throw new IllegalArgumentException("allocatedAmountCents must be between zero and the target amount");
        }
    }

    public long remainingAmountCents() {
        return targetAmountCents - allocatedAmountCents;
    }
}
