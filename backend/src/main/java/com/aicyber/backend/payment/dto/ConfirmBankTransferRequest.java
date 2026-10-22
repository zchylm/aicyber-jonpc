package com.aicyber.backend.payment.dto;

import java.time.OffsetDateTime;

public record ConfirmBankTransferRequest(
        String bankTransactionReference,
        OffsetDateTime bankReceivedAt,
        String reviewNote
) {}
