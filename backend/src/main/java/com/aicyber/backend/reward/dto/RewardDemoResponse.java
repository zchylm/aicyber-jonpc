package com.aicyber.backend.reward.dto;

public record RewardDemoResponse(
        String action,
        String message,
        RewardPublicSummaryResponse summary,
        RewardMeResponse member
) {
}
