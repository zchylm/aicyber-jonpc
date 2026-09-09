package com.aicyber.backend.payment.service;

import com.aicyber.backend.payment.dto.MockPaymentResponse;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.model.PaymentSettlement;
import com.aicyber.backend.reward.dto.RewardEntryResponse;
import com.aicyber.backend.reward.dto.RewardMeResponse;
import com.aicyber.backend.reward.service.RewardEventProcessor;
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
    private final RewardEventProcessor rewardEventProcessor;
    private final RewardQueryService rewardQueryService;

    public MockPaymentWorkflowService(
            PaymentService paymentService,
            MockPaymentSettlementService settlementService,
            RewardEventProcessor rewardEventProcessor,
            RewardQueryService rewardQueryService
    ) {
        this.paymentService = paymentService;
        this.settlementService = settlementService;
        this.rewardEventProcessor = rewardEventProcessor;
        this.rewardQueryService = rewardQueryService;
    }

    public MockPaymentResponse create(UUID userId, String requestReference, String idempotencyKey) {
        Payment payment = paymentService.createMockCheckout(userId, requestReference, idempotencyKey);
        if (!"SUCCEEDED".equals(payment.status())) {
            return response(payment, "NOT_ELIGIBLE", null);
        }
        RewardEntryResponse reward = rewardFor(userId, payment.orderReference());
        return response(payment, reward == null ? "PROCESSING" : "JOINED", reward);
    }

    public MockPaymentResponse complete(UUID userId, UUID paymentId, String outcome) {
        if ("FAILED".equalsIgnoreCase(outcome)) {
            return response(settlementService.fail(userId, paymentId), "NOT_ELIGIBLE", null);
        }
        if (!"SUCCEEDED".equalsIgnoreCase(outcome)) {
            throw new IllegalArgumentException("Payment outcome must be SUCCEEDED or FAILED");
        }

        PaymentSettlement settlement = settlementService.succeed(userId, paymentId);
        if (settlement.newlySucceeded()) {
            try {
                rewardEventProcessor.processNext(settlement.rewardProgramId());
            } catch (RuntimeException exception) {
                LOGGER.error("Payment {} succeeded but reward processing remains pending", paymentId, exception);
            }
        }

        RewardEntryResponse reward = rewardFor(userId, settlement.payment().orderReference());
        return response(settlement.payment(), reward == null ? "PROCESSING" : "JOINED", reward);
    }

    private RewardEntryResponse rewardFor(UUID userId, String orderReference) {
        RewardMeResponse member = rewardQueryService.memberSummary(userId);
        return member.entries().stream()
                .filter(entry -> entry.orderReference().equals(orderReference))
                .findFirst()
                .orElse(null);
    }

    private MockPaymentResponse response(Payment payment, String rewardState, RewardEntryResponse reward) {
        return new MockPaymentResponse(
                payment.id(),
                payment.paymentReference(),
                payment.orderId(),
                payment.orderReference(),
                payment.amountCents(),
                payment.currency(),
                payment.status(),
                payment.failureReason(),
                rewardState,
                reward
        );
    }
}
