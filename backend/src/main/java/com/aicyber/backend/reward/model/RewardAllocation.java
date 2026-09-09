package com.aicyber.backend.reward.model;

import java.util.Objects;
import java.util.UUID;

public record RewardAllocation(UUID queueEntryId, long amountCents) {
    public RewardAllocation {
        Objects.requireNonNull(queueEntryId, "queueEntryId is required");
        if (amountCents <= 0) {
            throw new IllegalArgumentException("amountCents must be positive");
        }
    }
}
