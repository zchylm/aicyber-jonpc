package com.aicyber.backend.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BankTransferProperties {
    private final boolean enabled;
    private final long reservationHours;
    private final String bankName;
    private final String accountName;
    private final String bsb;
    private final String accountNumber;

    public BankTransferProperties(
            @Value("${jonpc.payments.bank-transfer.enabled:false}") boolean enabled,
            @Value("${jonpc.payments.bank-transfer.reservation-hours:24}") long reservationHours,
            @Value("${jonpc.payments.bank-transfer.bank-name:}") String bankName,
            @Value("${jonpc.payments.bank-transfer.account-name:}") String accountName,
            @Value("${jonpc.payments.bank-transfer.bsb:}") String bsb,
            @Value("${jonpc.payments.bank-transfer.account-number:}") String accountNumber
    ) {
        this.enabled = enabled;
        this.reservationHours = reservationHours;
        this.bankName = bankName.trim();
        this.accountName = accountName.trim();
        this.bsb = bsb.trim();
        this.accountNumber = accountNumber.trim();
    }

    public void requireConfigured() {
        if (!enabled) throw new IllegalStateException("Bank transfer checkout is not available");
        if (reservationHours < 1 || bankName.isBlank() || accountName.isBlank() || bsb.isBlank() || accountNumber.isBlank()) {
            throw new IllegalStateException("Bank transfer checkout is not fully configured");
        }
    }

    public long reservationHours() { return reservationHours; }
    public boolean enabled() { return enabled; }
    public String bankName() { return bankName; }
    public String accountName() { return accountName; }
    public String bsb() { return bsb; }
    public String accountNumber() { return accountNumber; }
}
