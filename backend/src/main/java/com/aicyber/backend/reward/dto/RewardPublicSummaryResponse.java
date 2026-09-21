package com.aicyber.backend.reward.dto;

public record RewardPublicSummaryResponse(
        boolean available,
        String programName,
        String programStatus,
        String currency,
        int maxPositions,
        long confirmedCount,
        long remainingPositions,
        String currentTierName,
        Integer currentRateBasisPoints,
        Long currentCapCents,
        long currentTierRemaining
) {
    public static RewardPublicSummaryResponse unavailable() {
        return new RewardPublicSummaryResponse(false, null, null, "AUD", 0, 0, 0, null, null, null, 0);
    }
}
