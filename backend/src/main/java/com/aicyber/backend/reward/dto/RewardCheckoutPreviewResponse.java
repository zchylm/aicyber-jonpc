package com.aicyber.backend.reward.dto;

public record RewardCheckoutPreviewResponse(
        boolean available,
        String reason,
        String tierName,
        Integer rateBasisPoints,
        Long capCents,
        Long cashbackAmountCents,
        Long effectivePriceCents,
        long remainingPositions
) {
    public static RewardCheckoutPreviewResponse unavailable(String reason, long remainingPositions) {
        return new RewardCheckoutPreviewResponse(
                false, reason, null, null, null, null, null, remainingPositions
        );
    }
}
