package com.aicyber.backend.payment.dto;

import com.aicyber.backend.reward.dto.RewardCheckoutPreviewResponse;
import com.aicyber.backend.reward.dto.RewardEntryResponse;

import java.util.UUID;

public record StripePaymentResponse(
        UUID paymentId,
        String paymentReference,
        UUID orderId,
        String orderReference,
        long amountCents,
        String currency,
        String status,
        String failureReason,
        String checkoutUrl,
        UUID invoiceId,
        String invoiceNumber,
        String rewardState,
        RewardCheckoutPreviewResponse rewardPreview,
        RewardEntryResponse reward
) {}
