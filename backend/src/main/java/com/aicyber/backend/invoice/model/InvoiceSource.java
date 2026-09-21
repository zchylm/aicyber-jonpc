package com.aicyber.backend.invoice.model;

import java.util.UUID;

public record InvoiceSource(
        UUID orderId,
        UUID paymentId,
        UUID userId,
        String orderReference,
        String paymentReference,
        long orderAmountCents,
        long paymentAmountCents,
        String orderCurrency,
        String paymentCurrency,
        String orderStatus,
        String paymentStatus,
        String buyerName,
        String buyerEmail,
        String buyerCustomerReference,
        String buyerLocation,
        String direction,
        String systemSku,
        String systemName,
        Integer founderNumber,
        String founderTierName,
        Long founderCashbackAmountCents
) {
}
