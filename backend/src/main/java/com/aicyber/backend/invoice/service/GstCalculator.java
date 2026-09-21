package com.aicyber.backend.invoice.service;

import com.aicyber.backend.invoice.model.GstBreakdown;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class GstCalculator {
    private static final BigDecimal GST_INCLUSIVE_DIVISOR = BigDecimal.valueOf(11);

    public GstBreakdown fromGstInclusiveTotal(long totalCents) {
        if (totalCents <= 0) throw new IllegalArgumentException("Invoice total must be positive");
        long gstCents = BigDecimal.valueOf(totalCents)
                .divide(GST_INCLUSIVE_DIVISOR, 0, RoundingMode.HALF_UP)
                .longValueExact();
        return new GstBreakdown(totalCents - gstCents, gstCents, totalCents);
    }
}
