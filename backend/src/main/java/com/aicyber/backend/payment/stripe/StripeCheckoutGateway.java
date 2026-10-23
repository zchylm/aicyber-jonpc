package com.aicyber.backend.payment.stripe;

import com.aicyber.backend.payment.model.Payment;

public interface StripeCheckoutGateway {
    CheckoutSession create(Payment payment, String customerEmail, long expiresAtEpochSeconds);
    CheckoutSession retrieve(String checkoutSessionId);
    CheckoutEvent parseWebhook(String payload, String signature);

    record CheckoutSession(String id, String url, String paymentStatus, String paymentIntentId) {}

    record CheckoutEvent(
            String id,
            String type,
            String checkoutSessionId,
            String paymentStatus,
            String paymentIntentId,
            Long amountTotal,
            String currency
    ) {}
}
