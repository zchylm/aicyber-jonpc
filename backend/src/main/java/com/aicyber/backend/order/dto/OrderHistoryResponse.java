package com.aicyber.backend.order.dto;

import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.reward.dto.RewardEntryResponse;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OrderHistoryResponse(
        UUID id,
        String requestReference,
        String direction,
        int estimatedPrice,
        int recommendedBaseline,
        int selectedAdjustments,
        String status,
        OffsetDateTime createdAt,
        ConfiguratorQuoteRequest configuration,
        UUID salesOrderId,
        String orderReference,
        String orderStatus,
        String paymentReference,
        String paymentStatus,
        UUID invoiceId,
        String invoiceNumber,
        OffsetDateTime invoiceIssuedAt,
        UUID quoteId,
        String quoteStatus,
        Long quoteTotalCents,
        OffsetDateTime quoteValidUntil,
        String quoteNote,
        RewardEntryResponse reward
) {
}
