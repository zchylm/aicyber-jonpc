package com.aicyber.backend.reward.model;

import java.util.UUID;

public record RewardProgramInfo(
        UUID id,
        String code,
        String name,
        String currency,
        String status,
        int maxPositions,
        long maxLiabilityCents
) {
}
