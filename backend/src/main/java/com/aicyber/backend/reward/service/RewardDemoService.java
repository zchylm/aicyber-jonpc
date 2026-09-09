package com.aicyber.backend.reward.service;

import com.aicyber.backend.order.model.SalesOrder;
import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.reward.dto.RewardDemoResponse;
import com.aicyber.backend.reward.repository.RewardDemoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("local")
@ConditionalOnProperty(name = "jonpc.rewards.demo-enabled", havingValue = "true")
public class RewardDemoService {
    private static final long DEMO_ORDER_AMOUNT_CENTS = 200_000;
    private static final String RESET_CONFIRMATION = "RESET_LOCAL_REWARD_DEMO";

    private final RewardDemoRepository demoRepository;
    private final SalesOrderService salesOrderService;
    private final RewardEventIntakeService eventIntakeService;
    private final RewardEventProcessor eventProcessor;
    private final RewardQueryService queryService;

    public RewardDemoService(
            RewardDemoRepository demoRepository,
            SalesOrderService salesOrderService,
            RewardEventIntakeService eventIntakeService,
            RewardEventProcessor eventProcessor,
            RewardQueryService queryService
    ) {
        this.demoRepository = demoRepository;
        this.salesOrderService = salesOrderService;
        this.eventIntakeService = eventIntakeService;
        this.eventProcessor = eventProcessor;
        this.queryService = queryService;
    }

    public RewardDemoResponse qualifyLatestBuildRequest(UUID userId) {
        UUID programId = demoRepository.ensureProgram();
        SalesOrder order = salesOrderService.createFromLatestBuildRequest(userId);
        completeEligibility(programId, order.id());
        return response(
                "QUALIFY_BUILD_REQUEST",
                order.orderReference() + " joined the queue using the confirmed demo amount.",
                userId
        );
    }

    public RewardDemoResponse simulateIncomingOrder(UUID currentUserId) {
        UUID programId = demoRepository.ensureProgram();
        UUID sourceUserId = demoRepository.createSyntheticCustomer();
        SalesOrder order = salesOrderService.createStandalone(sourceUserId, DEMO_ORDER_AMOUNT_CENTS);
        completeEligibility(programId, order.id());
        return response(
                "SIMULATE_INCOMING_ORDER",
                "A $2,000 demo order created a $500 FIFO contribution.",
                currentUserId
        );
    }

    public RewardDemoResponse reset(UUID currentUserId, String confirmation) {
        if (!RESET_CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException("Local reward reset confirmation did not match");
        }
        demoRepository.requireLocalDatabase();
        UUID programId = demoRepository.ensureProgram();
        int removedOrders = demoRepository.resetProgramData(programId);
        return response(
                "RESET_LOCAL_REWARD_DEMO",
                "Local reward demo reset. Removed " + removedOrders + " demo Sales Orders.",
                currentUserId
        );
    }

    private void completeEligibility(UUID programId, UUID orderId) {
        salesOrderService.markPaid(orderId);
        salesOrderService.markRewardEligible(orderId);
        if (!eventIntakeService.recordOrderEligible(programId, orderId)) {
            throw new IllegalStateException("Reward eligibility event already exists");
        }
        if (!eventProcessor.processNext(programId)) {
            throw new IllegalStateException("Reward eligibility event was not processed");
        }
    }

    private RewardDemoResponse response(String action, String message, UUID userId) {
        return new RewardDemoResponse(
                action,
                message,
                queryService.publicSummary(),
                queryService.memberSummary(userId)
        );
    }
}
