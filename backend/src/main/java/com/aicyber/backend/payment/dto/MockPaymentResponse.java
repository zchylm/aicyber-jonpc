package com.aicyber.backend.payment.dto;

import com.aicyber.backend.reward.dto.RewardEntryResponse;
import com.aicyber.backend.reward.dto.RewardCheckoutPreviewResponse;

import java.util.UUID;

public record MockPaymentResponse(
        UUID paymentId,
        String paymentReference,
        UUID orderId,
        String orderReference,
        long amountCents,
        String currency,
        String status,
        String failureReason,
        UUID invoiceId,
        String invoiceNumber,
        String rewardState,
        RewardCheckoutPreviewResponse rewardPreview,
        RewardEntryResponse reward
) {
}
