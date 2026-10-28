package com.aicyber.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AdminRewardSnapshotResponse(
        CampaignSummary campaign,
        List<CommitmentItem> commitments
) {
    public record CampaignSummary(
            int maxPositions,
            long confirmedPositions,
            long availablePositions,
            long maxLiabilityCents,
            long committedCents,
            long paidCents,
            long availableBudgetCents
    ) {}

    public record CommitmentItem(
            UUID id,
            long founderNumber,
            String tierName,
            String orderReference,
            String customerEmail,
            long purchaseAmountCents,
            int rateBasisPoints,
            long capCents,
            long cashbackAmountCents,
            String paymentProvider,
            String status,
            OffsetDateTime lockedAt,
            OffsetDateTime paidAt
    ) {}
}
