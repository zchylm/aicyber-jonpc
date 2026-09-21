package com.aicyber.backend.invoice.model;

public record GstBreakdown(long subtotalExGstCents, long gstCents, long totalCents) {
}
