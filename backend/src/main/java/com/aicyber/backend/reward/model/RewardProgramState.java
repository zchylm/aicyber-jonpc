package com.aicyber.backend.reward.model;

import java.util.UUID;

public record RewardProgramState(
        UUID id,
        String currency,
        String status,
        long nextQueueSequence,
        int maxPositions,
        long maxLiabilityCents
) {
}
