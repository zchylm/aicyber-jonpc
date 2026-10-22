package com.aicyber.backend.payment.dto;

import com.aicyber.backend.reward.dto.RewardCheckoutPreviewResponse;
import com.aicyber.backend.reward.dto.RewardEntryResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record BankTransferResponse(
        UUID paymentId,
        String paymentReference,
        UUID orderId,
        String orderReference,
        long amountCents,
        String currency,
        String status,
        String failureReason,
        OffsetDateTime expiresAt,
        OffsetDateTime customerReportedAt,
        String bankName,
        String accountName,
        String bsb,
        String accountNumber,
        UUID invoiceId,
        String invoiceNumber,
        String rewardState,
        RewardCheckoutPreviewResponse rewardPreview,
        RewardEntryResponse reward
) {}
