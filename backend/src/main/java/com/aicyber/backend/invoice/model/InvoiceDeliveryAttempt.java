package com.aicyber.backend.invoice.model;

import java.util.UUID;

public record InvoiceDeliveryAttempt(UUID id, UUID invoiceId, String recipientEmail, String status, int attemptCount) {
}
