package com.aicyber.backend.payment.stripe;

import com.aicyber.backend.payment.config.StripeProperties;
import com.aicyber.backend.payment.model.Payment;
import com.stripe.StripeClient;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class StripeJavaCheckoutGateway implements StripeCheckoutGateway {
    private final StripeProperties properties;

    public StripeJavaCheckoutGateway(StripeProperties properties) {
        this.properties = properties;
    }

    @Override
    public CheckoutSession create(Payment payment, String customerEmail, long expiresAtEpochSeconds) {
        properties.requireCheckoutConfigured();
        String paymentId = payment.id().toString();
        SessionCreateParams.PaymentIntentData intentData = SessionCreateParams.PaymentIntentData.builder()
                .putMetadata("jonpc_payment_id", paymentId)
                .putMetadata("jonpc_order_id", payment.orderId().toString())
                .putMetadata("jonpc_order_reference", payment.orderReference())
                .build();
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSubmitType(SessionCreateParams.SubmitType.PAY)
                .setCustomerEmail(customerEmail)
                .setClientReferenceId(payment.orderReference())
                .setSuccessUrl(properties.returnUrl("success", payment.id()))
                .setCancelUrl(properties.returnUrl("cancelled", payment.id()))
                .setExpiresAt(expiresAtEpochSeconds)
                .setWalletOptions(SessionCreateParams.WalletOptions.builder()
                        .setLink(SessionCreateParams.WalletOptions.Link.builder()
                                .setDisplay(SessionCreateParams.WalletOptions.Link.Display.NEVER)
                                .build())
                        .build())
                .setPaymentIntentData(intentData)
                .putMetadata("jonpc_payment_id", paymentId)
                .putMetadata("jonpc_order_id", payment.orderId().toString());
        long productPriceCents = payment.productSubtotalCents() - payment.discountCents();
        builder.addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(payment.currency().toLowerCase(Locale.ROOT))
                                .setUnitAmount(productPriceCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("JON.PC order " + payment.orderReference())
                                        .setDescription(payment.discountCents() > 0
                                                ? "Original " + money(payment.productSubtotalCents()) + " · Founder 5% discount "
                                                    + "-" + money(payment.discountCents()) + " · GST included"
                                                : "Complete system order · GST included")
                                        .build())
                                .build())
                        .build());
        if (payment.shippingFeeCents() > 0) {
            builder.addLineItem(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(payment.currency().toLowerCase(Locale.ROOT))
                            .setUnitAmount(payment.shippingFeeCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Melbourne delivery")
                                    .setDescription("Flat-rate delivery · GST included")
                                    .build())
                            .build())
                    .build());
        }
        SessionCreateParams params = builder.build();
        try {
            StripeClient client = new StripeClient(properties.secretKey());
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("jonpc-checkout-" + payment.id())
                    .build();
            return map(client.v1().checkout().sessions().create(params, options));
        } catch (StripeException exception) {
            throw new IllegalStateException("Stripe could not prepare the secure payment page", exception);
        }
    }

    @Override
    public CheckoutSession retrieve(String checkoutSessionId) {
        properties.requireCheckoutConfigured();
        try {
            StripeClient client = new StripeClient(properties.secretKey());
            return map(client.v1().checkout().sessions().retrieve(checkoutSessionId));
        } catch (StripeException exception) {
            throw new IllegalStateException("Stripe Checkout session could not be retrieved", exception);
        }
    }

    @Override
    public CheckoutSession expire(String checkoutSessionId) {
        properties.requireCheckoutConfigured();
        try {
            StripeClient client = new StripeClient(properties.secretKey());
            return map(client.v1().checkout().sessions().expire(checkoutSessionId));
        } catch (StripeException exception) {
            throw new IllegalStateException("Stripe Checkout session could not be cancelled", exception);
        }
    }

    @Override
    public CheckoutEvent parseWebhook(String payload, String signature) {
        properties.requireWebhookConfigured();
        Event event;
        try {
            event = Webhook.constructEvent(payload, signature, properties.webhookSecret());
        } catch (SignatureVerificationException exception) {
            throw new IllegalArgumentException("Stripe webhook signature is invalid", exception);
        }
        Object object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(object instanceof Session session)) {
            return new CheckoutEvent(event.getId(), event.getType(), null, null, null, null, null);
        }
        return new CheckoutEvent(
                event.getId(), event.getType(), session.getId(), session.getPaymentStatus(),
                session.getPaymentIntent(), session.getAmountTotal(), session.getCurrency()
        );
    }

    private CheckoutSession map(Session session) {
        return new CheckoutSession(session.getId(), session.getUrl(), session.getStatus(), session.getPaymentStatus(), session.getPaymentIntent());
    }

    private String money(long cents) {
        return String.format(Locale.ROOT, "A$%.2f", cents / 100.0);
    }
}
