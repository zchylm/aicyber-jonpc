package com.aicyber.backend.payment.dto;

public record CreateStripeCheckoutRequest(String requestReference, String idempotencyKey) {}
