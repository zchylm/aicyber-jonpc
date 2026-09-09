package com.aicyber.backend.reward.dto;

import java.util.List;

public record RewardMeResponse(
        String state,
        String programStatus,
        String currency,
        List<RewardEntryResponse> entries
) {
}
