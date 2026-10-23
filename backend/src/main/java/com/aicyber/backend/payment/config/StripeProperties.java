package com.aicyber.backend.payment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;

@Component
public class StripeProperties {
    private final boolean enabled;
    private final String secretKey;
    private final String webhookSecret;
    private final long checkoutMinutes;
    private final String frontendUrl;

    public StripeProperties(
            @Value("${jonpc.payments.stripe.enabled:false}") boolean enabled,
            @Value("${jonpc.payments.stripe.secret-key:}") String secretKey,
            @Value("${jonpc.payments.stripe.webhook-secret:}") String webhookSecret,
            @Value("${jonpc.payments.stripe.checkout-minutes:30}") long checkoutMinutes,
            @Value("${jonpc.payments.stripe.frontend-url:http://localhost:5174/}") String frontendUrl
    ) {
        this.enabled = enabled;
        this.secretKey = secretKey.trim();
        this.webhookSecret = webhookSecret.trim();
        this.checkoutMinutes = checkoutMinutes;
        this.frontendUrl = frontendUrl.trim();
    }

    public void requireCheckoutConfigured() {
        if (!enabled) throw new IllegalStateException("Stripe checkout is not available");
        if (!(secretKey.startsWith("sk_test_") || secretKey.startsWith("sk_live_"))) {
            throw new IllegalStateException("Stripe checkout is not fully configured");
        }
        if (checkoutMinutes < 30 || checkoutMinutes > 1440) {
            throw new IllegalStateException("Stripe checkout window must be between 30 minutes and 24 hours");
        }
        URI uri;
        try {
            uri = URI.create(frontendUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Stripe frontend URL is invalid", exception);
        }
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())) || uri.getHost() == null) {
            throw new IllegalStateException("Stripe frontend URL is invalid");
        }
    }

    public void requireWebhookConfigured() {
        if (!enabled || !webhookSecret.startsWith("whsec_")) {
            throw new IllegalStateException("Stripe webhook is not fully configured");
        }
    }

    public boolean enabled() { return enabled; }
    public String secretKey() { return secretKey; }
    public String webhookSecret() { return webhookSecret; }
    public long checkoutMinutes() { return checkoutMinutes; }

    public String returnUrl(String result, java.util.UUID paymentId) {
        String separator = frontendUrl.contains("?") ? "&" : "?";
        return frontendUrl + separator + "stripe_result=" + result + "&stripe_payment=" + paymentId;
    }
}
