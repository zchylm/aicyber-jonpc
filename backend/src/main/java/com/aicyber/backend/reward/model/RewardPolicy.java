package com.aicyber.backend.reward.model;

import java.util.UUID;

public record RewardPolicy(
        UUID id,
        String calculationType,
        Integer rateBasisPoints,
        Long fixedAmountCents
) {
}
