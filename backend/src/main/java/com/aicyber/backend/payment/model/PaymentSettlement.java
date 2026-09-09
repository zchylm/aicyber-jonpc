package com.aicyber.backend.payment.model;

import java.util.UUID;

public record PaymentSettlement(Payment payment, UUID rewardProgramId, boolean newlySucceeded) {
}
