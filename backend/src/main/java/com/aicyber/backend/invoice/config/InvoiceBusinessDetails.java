package com.aicyber.backend.invoice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record InvoiceBusinessDetails(
        String legalName,
        String tradingName,
        String abn,
        String address,
        String email,
        String phone
) {
    public InvoiceBusinessDetails(
            @Value("${jonpc.invoice.seller-legal-name}") String legalName,
            @Value("${jonpc.invoice.seller-trading-name}") String tradingName,
            @Value("${jonpc.invoice.seller-abn}") String abn,
            @Value("${jonpc.invoice.seller-address}") String address,
            @Value("${jonpc.invoice.seller-email}") String email,
            @Value("${jonpc.invoice.seller-phone}") String phone
    ) {
        this.legalName = require(legalName, "seller legal name");
        this.tradingName = require(tradingName, "seller trading name");
        this.abn = validAbn(abn);
        this.address = require(address, "seller address");
        this.email = require(email, "seller email");
        this.phone = require(phone, "seller phone");
    }

    private static String require(String value, String label) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Invoice " + label + " is required");
        return value.trim();
    }

    private static String validAbn(String value) {
        String formatted = require(value, "seller ABN");
        if (!formatted.replaceAll("\\D", "").matches("\\d{11}")) {
            throw new IllegalArgumentException("Invoice seller ABN must contain 11 digits");
        }
        return formatted;
    }
}
