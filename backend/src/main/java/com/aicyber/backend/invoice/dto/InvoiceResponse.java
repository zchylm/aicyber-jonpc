package com.aicyber.backend.invoice.dto;

import com.aicyber.backend.invoice.model.SalesInvoice;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        UUID orderId,
        String invoiceNumber,
        String documentType,
        String status,
        String orderReference,
        String paymentReference,
        String customerReference,
        String currency,
        long subtotalExGstCents,
        long gstCents,
        long totalCents,
        long amountPaidCents,
        OffsetDateTime issuedAt
) {
    public static InvoiceResponse from(SalesInvoice invoice) {
        return new InvoiceResponse(
                invoice.id(), invoice.orderId(), invoice.invoiceNumber(), invoice.documentType(), invoice.status(),
                invoice.orderReference(), invoice.paymentReference(), invoice.buyerCustomerReference(), invoice.currency(),
                invoice.subtotalExGstCents(), invoice.gstCents(), invoice.totalCents(),
                invoice.amountPaidCents(), invoice.issuedAt()
        );
    }
}
