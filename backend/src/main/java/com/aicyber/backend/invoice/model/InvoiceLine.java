package com.aicyber.backend.invoice.model;

import java.util.UUID;

public record InvoiceLine(
        UUID id,
        int lineNumber,
        String sku,
        String description,
        int quantity,
        long unitPriceExGstCents,
        int gstRateBasisPoints,
        long gstCents,
        long lineTotalCents,
        boolean taxable
) {
}
