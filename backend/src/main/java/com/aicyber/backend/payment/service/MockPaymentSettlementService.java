package com.aicyber.backend.payment.service;

import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.model.PaymentSettlement;
import com.aicyber.backend.payment.repository.PaymentRepository;
import com.aicyber.backend.reward.repository.RewardDemoRepository;
import com.aicyber.backend.reward.service.RewardEventIntakeService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Profile("local")
@ConditionalOnProperty(name = "jonpc.payments.demo-enabled", havingValue = "true")
public class MockPaymentSettlementService {
    private static final String DEMO_DECLINE_REASON = "Demo payment was declined";

    private final PaymentRepository paymentRepository;
    private final SalesOrderService salesOrderService;
    private final RewardDemoRepository rewardDemoRepository;
    private final RewardEventIntakeService rewardEventIntakeService;

    public MockPaymentSettlementService(
            PaymentRepository paymentRepository,
            SalesOrderService salesOrderService,
            RewardDemoRepository rewardDemoRepository,
            RewardEventIntakeService rewardEventIntakeService
    ) {
        this.paymentRepository = paymentRepository;
        this.salesOrderService = salesOrderService;
        this.rewardDemoRepository = rewardDemoRepository;
        this.rewardEventIntakeService = rewardEventIntakeService;
    }

    @Transactional
    public PaymentSettlement succeed(UUID userId, UUID paymentId) {
        Payment payment = ownedPayment(userId, paymentId);
        UUID programId = rewardDemoRepository.ensureProgram();
        if ("SUCCEEDED".equals(payment.status())) {
            return new PaymentSettlement(payment, programId, false);
        }
        if (!"PENDING_PAYMENT".equals(payment.orderStatus())) {
            Payment succeeded = paymentRepository.findSuccessfulByOrder(payment.orderId())
                    .orElseThrow(() -> new IllegalStateException("Sales order is no longer awaiting payment"));
            return new PaymentSettlement(succeeded, programId, false);
        }
        if (!"CREATED".equals(payment.status())) {
            throw new IllegalStateException("Create a new payment attempt before trying again");
        }

        Payment succeeded = paymentRepository.markSucceeded(payment.id());
        salesOrderService.markPaid(payment.orderId());
        salesOrderService.markRewardEligible(payment.orderId());
        if (!rewardEventIntakeService.recordOrderEligible(programId, payment.orderId())) {
            throw new IllegalStateException("Reward eligibility was already recorded for this order");
        }
        return new PaymentSettlement(succeeded, programId, true);
    }

    @Transactional
    public Payment fail(UUID userId, UUID paymentId) {
        Payment payment = ownedPayment(userId, paymentId);
        if ("FAILED".equals(payment.status())) {
            return payment;
        }
        if (!"CREATED".equals(payment.status())) {
            throw new IllegalStateException("Payment can no longer be declined");
        }
        return paymentRepository.markFailed(payment.id(), DEMO_DECLINE_REASON);
    }

    private Payment ownedPayment(UUID userId, UUID paymentId) {
        return paymentRepository.lockOwned(paymentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Payment was not found for this account"));
    }
}
