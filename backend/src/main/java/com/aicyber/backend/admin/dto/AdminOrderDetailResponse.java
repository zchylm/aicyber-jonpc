package com.aicyber.backend.admin.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record AdminOrderDetailResponse(
        UUID id,
        String orderReference,
        String customerName,
        String customerEmail,
        String customerReference,
        long amountCents,
        String currency,
        String status,
        OffsetDateTime paidAt,
        OffsetDateTime rewardEligibleAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        String buildRequestReference,
        String direction,
        Map<String, Object> configuration,
        DeliveryItem delivery,
        List<PaymentItem> payments,
        InvoiceItem invoice,
        RewardItem reward
) {
    public record DeliveryItem(
            String recipientName, String phone, String addressLine1, String addressLine2,
            String suburb, String state, String postcode, String country
    ) {}

    public record PaymentItem(
            UUID id,
            String paymentReference,
            String provider,
            String providerPaymentId,
            long amountCents,
            String currency,
            String status,
            String failureReason,
            OffsetDateTime createdAt,
            OffsetDateTime succeededAt
    ) {
    }

    public record InvoiceItem(
            UUID id,
            String invoiceNumber,
            String status,
            long subtotalExGstCents,
            long gstCents,
            long totalCents,
            OffsetDateTime issuedAt,
            String deliveryStatus,
            OffsetDateTime sentAt
    ) {
    }

    public record RewardItem(
            UUID id,
            long founderNumber,
            String tierName,
            int rateBasisPoints,
            long capCents,
            long purchaseAmountCents,
            long cashbackAmountCents,
            String status,
            OffsetDateTime lockedAt,
            OffsetDateTime payableAt,
            OffsetDateTime paidAt
    ) {
    }
}
