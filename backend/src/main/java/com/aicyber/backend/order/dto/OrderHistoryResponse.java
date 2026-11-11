package com.aicyber.backend.order.dto;

import com.aicyber.backend.configurator.dto.ConfiguratorSelection;
import com.aicyber.backend.reward.dto.RewardEntryResponse;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;

public record OrderHistoryResponse(
        UUID id,
        String requestReference,
        String direction,
        Long productSubtotalCents,
        String status,
        OffsetDateTime createdAt,
        ConfiguratorSelection configuration,
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
        OrderFulfilmentResponse fulfilment,
        RewardEntryResponse reward,
        List<OrderSystemItemResponse> items
) {
}
