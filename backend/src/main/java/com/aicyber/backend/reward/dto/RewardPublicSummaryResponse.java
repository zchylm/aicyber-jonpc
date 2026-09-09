package com.aicyber.backend.reward.dto;

public record RewardPublicSummaryResponse(
        boolean available,
        String programName,
        String programStatus,
        String currency,
        Integer contributionRateBasisPoints,
        long waitingCount,
        long completedCount,
        long totalAllocatedCents
) {
    public static RewardPublicSummaryResponse unavailable() {
        return new RewardPublicSummaryResponse(false, null, null, "AUD", null, 0, 0, 0);
    }
}
