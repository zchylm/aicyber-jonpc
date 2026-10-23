package com.aicyber.backend.payment.service;

import com.aicyber.backend.auth.model.User;
import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.service.InvoiceDeliveryService;
import com.aicyber.backend.invoice.service.InvoiceService;
import com.aicyber.backend.payment.config.StripeProperties;
import com.aicyber.backend.payment.dto.StripePaymentResponse;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.model.PaymentSettlement;
import com.aicyber.backend.payment.repository.PaymentRepository;
import com.aicyber.backend.payment.stripe.StripeCheckoutGateway;
import com.aicyber.backend.reward.dto.RewardEntryResponse;
import com.aicyber.backend.reward.dto.RewardMeResponse;
import com.aicyber.backend.reward.repository.RewardDemoRepository;
import com.aicyber.backend.reward.service.RewardQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class StripePaymentWorkflowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StripePaymentWorkflowService.class);
    private static final Set<String> HANDLED_EVENTS = Set.of(
            "checkout.session.completed",
            "checkout.session.async_payment_succeeded",
            "checkout.session.async_payment_failed",
            "checkout.session.expired"
    );

    private final StripeProperties properties;
    private final StripeCheckoutGateway stripe;
    private final PaymentService paymentService;
    private final PaymentRepository payments;
    private final StripePaymentSettlementService settlement;
    private final UserRepository users;
    private final RewardDemoRepository rewardDemoRepository;
    private final RewardQueryService rewardQueryService;
    private final InvoiceService invoiceService;
    private final InvoiceDeliveryService invoiceDeliveryService;

    public StripePaymentWorkflowService(
            StripeProperties properties,
            StripeCheckoutGateway stripe,
            PaymentService paymentService,
            PaymentRepository payments,
            StripePaymentSettlementService settlement,
            UserRepository users,
            RewardDemoRepository rewardDemoRepository,
            RewardQueryService rewardQueryService,
            InvoiceService invoiceService,
            InvoiceDeliveryService invoiceDeliveryService
    ) {
        this.properties = properties;
        this.stripe = stripe;
        this.paymentService = paymentService;
        this.payments = payments;
        this.settlement = settlement;
        this.users = users;
        this.rewardDemoRepository = rewardDemoRepository;
        this.rewardQueryService = rewardQueryService;
        this.invoiceService = invoiceService;
        this.invoiceDeliveryService = invoiceDeliveryService;
    }

    public StripePaymentResponse create(UUID userId, String requestReference, String idempotencyKey) {
        properties.requireCheckoutConfigured();
        rewardDemoRepository.ensureProgram();
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User account not found"));
        OffsetDateTime expiresAt = OffsetDateTime.now().plusMinutes(properties.checkoutMinutes());
        Payment payment = paymentService.createStripe(userId, requestReference, idempotencyKey, expiresAt);
        if (!"STRIPE".equals(payment.provider()) || "SUCCEEDED".equals(payment.status())) {
            return response(userId, payment, null);
        }
        try {
            StripeCheckoutGateway.CheckoutSession session;
            if (payment.providerCheckoutSessionId() == null) {
                session = stripe.create(payment, user.email(), expiresAt.toEpochSecond());
                payment = payments.attachStripeSession(payment.id(), session.id());
            } else {
                session = stripe.retrieve(payment.providerCheckoutSessionId());
            }
            if (session.url() == null || session.url().isBlank()) {
                settlement.cancel(payment.id());
                throw new IllegalStateException("Stripe Checkout session has expired. Start a new payment attempt.");
            }
            return response(userId, payment, session.url());
        } catch (RuntimeException exception) {
            settlement.fail(payment.id(), "Stripe Checkout could not be prepared");
            throw exception;
        }
    }

    public StripePaymentResponse status(UUID userId, UUID paymentId) {
        properties.requireCheckoutConfigured();
        Payment payment = payments.findOwned(paymentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Stripe payment was not found for this account"));
        if (!"STRIPE".equals(payment.provider())) throw new IllegalArgumentException("Payment is not a Stripe payment");
        String checkoutUrl = null;
        if ("CREATED".equals(payment.status()) && payment.providerCheckoutSessionId() != null) {
            checkoutUrl = stripe.retrieve(payment.providerCheckoutSessionId()).url();
        }
        return response(userId, payment, checkoutUrl);
    }

    public void handleWebhook(String payload, String signature) {
        StripeCheckoutGateway.CheckoutEvent event = stripe.parseWebhook(payload, signature);
        if (!HANDLED_EVENTS.contains(event.type())) return;
        if (event.checkoutSessionId() == null) throw new IllegalArgumentException("Stripe Checkout session is missing");
        Payment payment = payments.findByStripeSession(event.checkoutSessionId())
                .orElseThrow(() -> new IllegalArgumentException("Stripe Checkout session is not recognised"));
        validateAmount(payment, event);

        switch (event.type()) {
            case "checkout.session.completed" -> {
                if ("paid".equals(event.paymentStatus())) complete(payment, event.paymentIntentId());
                else settlement.markProcessing(payment.id());
            }
            case "checkout.session.async_payment_succeeded" -> complete(payment, event.paymentIntentId());
            case "checkout.session.async_payment_failed" -> settlement.fail(payment.id(), "Stripe payment was not completed");
            case "checkout.session.expired" -> settlement.cancel(payment.id());
            default -> { }
        }
    }

    private void complete(Payment payment, String paymentIntentId) {
        PaymentSettlement result = settlement.succeed(payment.id(), paymentIntentId);
        if (!result.newlySucceeded()) return;
        try {
            invoiceDeliveryService.deliver(result.invoiceId());
        } catch (RuntimeException exception) {
            LOGGER.error("Invoice {} was issued but email delivery remains pending", result.invoiceId(), exception);
        }
    }

    private void validateAmount(Payment payment, StripeCheckoutGateway.CheckoutEvent event) {
        if (event.amountTotal() == null || event.amountTotal() != payment.amountCents()) {
            throw new IllegalStateException("Stripe payment amount does not match the order");
        }
        if (event.currency() == null || !event.currency().toUpperCase(Locale.ROOT).equals(payment.currency())) {
            throw new IllegalStateException("Stripe payment currency does not match the order");
        }
    }

    private StripePaymentResponse response(UUID userId, Payment payment, String checkoutUrl) {
        SalesInvoice invoice = invoiceService.invoiceForOrder(payment.orderId()).orElse(null);
        RewardEntryResponse reward = rewardFor(userId, payment.orderReference());
        return new StripePaymentResponse(
                payment.id(), payment.paymentReference(), payment.orderId(), payment.orderReference(),
                payment.amountCents(), payment.currency(), payment.status(), payment.failureReason(), checkoutUrl,
                invoice == null ? null : invoice.id(), invoice == null ? null : invoice.invoiceNumber(),
                reward == null ? ("SUCCEEDED".equals(payment.status()) ? "PROCESSING" : "NOT_ELIGIBLE") : "JOINED",
                "CREATED".equals(payment.status()) ? rewardQueryService.checkoutPreview(userId, payment.amountCents()) : null,
                reward
        );
    }

    private RewardEntryResponse rewardFor(UUID userId, String orderReference) {
        RewardMeResponse member = rewardQueryService.memberSummary(userId);
        return member.entries().stream().filter(entry -> entry.orderReference().equals(orderReference)).findFirst().orElse(null);
    }
}
