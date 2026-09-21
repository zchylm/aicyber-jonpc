package com.aicyber.backend.reward.model;

import java.util.UUID;

public record FounderTier(
        UUID id,
        String code,
        String displayName,
        int rateBasisPoints,
        long capCents
) {
}
