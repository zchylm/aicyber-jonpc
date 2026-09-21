package com.aicyber.backend.invoice.model;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SalesInvoice(
        UUID id,
        UUID orderId,
        UUID paymentId,
        UUID userId,
        String invoiceNumber,
        String documentType,
        String status,
        String orderReference,
        String paymentReference,
        String sellerLegalName,
        String sellerTradingName,
        String sellerAbn,
        String sellerAddress,
        String sellerEmail,
        String sellerPhone,
        String buyerName,
        String buyerEmail,
        String buyerCustomerReference,
        String buyerLocation,
        String buyerAbn,
        String currency,
        long subtotalExGstCents,
        long gstCents,
        long totalCents,
        long amountPaidCents,
        OffsetDateTime issuedAt,
        Integer founderNumber,
        String founderTierName,
        Long founderCashbackAmountCents,
        String founderCashbackTerms,
        List<InvoiceLine> lines,
        byte[] pdfContent,
        String pdfSha256
) {
    public SalesInvoice {
        lines = List.copyOf(lines);
        pdfContent = pdfContent == null ? null : pdfContent.clone();
    }

    @Override
    public byte[] pdfContent() {
        return pdfContent == null ? null : pdfContent.clone();
    }
}
