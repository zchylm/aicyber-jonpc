package com.aicyber.backend.payment.service;

import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.service.InvoiceDeliveryService;
import com.aicyber.backend.invoice.service.InvoiceService;
import com.aicyber.backend.payment.dto.MockPaymentResponse;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.model.PaymentSettlement;
import com.aicyber.backend.reward.dto.RewardEntryResponse;
import com.aicyber.backend.reward.dto.RewardCheckoutPreviewResponse;
import com.aicyber.backend.reward.dto.RewardMeResponse;
import com.aicyber.backend.reward.repository.RewardDemoRepository;
import com.aicyber.backend.reward.service.RewardQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("local")
@ConditionalOnProperty(name = "jonpc.payments.demo-enabled", havingValue = "true")
public class MockPaymentWorkflowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(MockPaymentWorkflowService.class);

    private final PaymentService paymentService;
    private final MockPaymentSettlementService settlementService;
    private final RewardQueryService rewardQueryService;
    private final RewardDemoRepository rewardDemoRepository;
    private final InvoiceService invoiceService;
    private final InvoiceDeliveryService invoiceDeliveryService;

    public MockPaymentWorkflowService(
            PaymentService paymentService,
            MockPaymentSettlementService settlementService,
            RewardQueryService rewardQueryService,
            RewardDemoRepository rewardDemoRepository,
            InvoiceService invoiceService,
            InvoiceDeliveryService invoiceDeliveryService
    ) {
        this.paymentService = paymentService;
        this.settlementService = settlementService;
        this.rewardQueryService = rewardQueryService;
        this.rewardDemoRepository = rewardDemoRepository;
        this.invoiceService = invoiceService;
        this.invoiceDeliveryService = invoiceDeliveryService;
    }

    public MockPaymentResponse create(UUID userId, String requestReference, String idempotencyKey) {
        rewardDemoRepository.ensureProgram();
        Payment payment = paymentService.createMockCheckout(userId, requestReference, idempotencyKey);
        if (!"SUCCEEDED".equals(payment.status())) {
            return response(payment, "NOT_ELIGIBLE", rewardQueryService.checkoutPreview(userId, payment.amountCents()), null);
        }
        RewardEntryResponse reward = rewardFor(userId, payment.orderReference());
        return response(payment, reward == null ? "PROCESSING" : "JOINED", null, reward);
    }

    public MockPaymentResponse complete(UUID userId, UUID paymentId, String outcome) {
        if ("FAILED".equalsIgnoreCase(outcome)) {
            Payment failed = settlementService.fail(userId, paymentId);
            return response(failed, "NOT_ELIGIBLE", null, null);
        }
        if (!"SUCCEEDED".equalsIgnoreCase(outcome)) {
            throw new IllegalArgumentException("Payment outcome must be SUCCEEDED or FAILED");
        }

        PaymentSettlement settlement = settlementService.succeed(userId, paymentId);
        if (settlement.newlySucceeded()) {
            try {
                invoiceDeliveryService.deliver(settlement.invoiceId());
            } catch (RuntimeException exception) {
                LOGGER.error("Invoice {} was issued but email delivery remains pending", settlement.invoiceId(), exception);
            }
        }

        RewardEntryResponse reward = rewardFor(userId, settlement.payment().orderReference());
        return response(settlement.payment(), reward == null ? "PROCESSING" : "JOINED", null, reward);
    }

    private RewardEntryResponse rewardFor(UUID userId, String orderReference) {
        RewardMeResponse member = rewardQueryService.memberSummary(userId);
        return member.entries().stream()
                .filter(entry -> entry.orderReference().equals(orderReference))
                .findFirst()
                .orElse(null);
    }

    private MockPaymentResponse response(
            Payment payment,
            String rewardState,
            RewardCheckoutPreviewResponse rewardPreview,
            RewardEntryResponse reward
    ) {
        SalesInvoice invoice = invoiceService.invoiceForOrder(payment.orderId()).orElse(null);
        return new MockPaymentResponse(
                payment.id(),
                payment.paymentReference(),
                payment.orderId(),
                payment.orderReference(),
                payment.amountCents(),
                payment.currency(),
                payment.status(),
                payment.failureReason(),
                invoice == null ? null : invoice.id(),
                invoice == null ? null : invoice.invoiceNumber(),
                rewardState,
                rewardPreview,
                reward
        );
    }
}
