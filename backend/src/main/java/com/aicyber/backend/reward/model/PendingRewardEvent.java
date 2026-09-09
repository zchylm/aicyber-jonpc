package com.aicyber.backend.reward.model;

import java.util.UUID;

public record PendingRewardEvent(UUID id, UUID orderId) {
}
