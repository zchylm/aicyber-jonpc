package com.aicyber.backend.payment.dto;

public record CreateMockPaymentRequest(String requestReference, String idempotencyKey) {
}
