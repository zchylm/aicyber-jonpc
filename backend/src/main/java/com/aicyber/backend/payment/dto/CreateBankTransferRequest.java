package com.aicyber.backend.payment.dto;

public record CreateBankTransferRequest(String requestReference, String idempotencyKey) {}
