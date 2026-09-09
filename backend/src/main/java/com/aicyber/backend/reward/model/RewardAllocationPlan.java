package com.aicyber.backend.reward.model;

import java.util.List;

public record RewardAllocationPlan(
        List<RewardAllocation> allocations,
        long remainingContributionCents
) {
    public RewardAllocationPlan {
        allocations = List.copyOf(allocations);
        if (remainingContributionCents < 0) {
            throw new IllegalArgumentException("remainingContributionCents cannot be negative");
        }
    }
}
