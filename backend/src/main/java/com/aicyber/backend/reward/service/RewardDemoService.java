package com.aicyber.backend.reward.service;

import com.aicyber.backend.order.model.SalesOrder;
import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.reward.dto.RewardDemoResponse;
import com.aicyber.backend.reward.repository.RewardDemoRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.DayOfWeek;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
@Profile("local")
@ConditionalOnProperty(name = "jonpc.rewards.demo-enabled", havingValue = "true")
public class RewardDemoService {
    private static final long DEMO_ORDER_AMOUNT_CENTS = 200_000;
    private static final String RESET_CONFIRMATION = "RESET_LOCAL_REWARD_DEMO";
    private static final ZoneId MELBOURNE = ZoneId.of("Australia/Melbourne");

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

    @Transactional
    public RewardDemoResponse qualifyLatestBuildRequest(UUID userId) {
        UUID programId = demoRepository.ensureProgram();
        SalesOrder order = salesOrderService.createFromLatestBuildRequest(userId);
        completeEligibility(programId, order.id());
        return response(
                "QUALIFY_BUILD_REQUEST",
                order.orderReference() + " received a locked Founder cashback commitment.",
                userId
        );
    }

    public RewardDemoResponse createFounderOrder(UUID currentUserId) {
        UUID programId = demoRepository.ensureProgram();
        UUID sourceUserId = demoRepository.createSyntheticCustomer();
        SalesOrder order = salesOrderService.createStandalone(sourceUserId, DEMO_ORDER_AMOUNT_CENTS);
        completeEligibility(programId, order.id());
        return response(
                "CREATE_FOUNDER_ORDER",
                "A new paid demo customer claimed the next Founder position.",
                currentUserId
        );
    }

    @Transactional
    public RewardDemoResponse makeLatestCashbackPayable(UUID userId) {
        demoRepository.requireLocalDatabase();
        UUID commitmentId = demoRepository.latestCommitment(userId, "LOCKED");
        demoRepository.markPayable(commitmentId, addBusinessDays(OffsetDateTime.now(MELBOURNE), 5));
        return response(
                "MAKE_CASHBACK_PAYABLE",
                "Confirmed delivery and validation were simulated. Cashback is ready for payout.",
                userId
        );
    }

    @Transactional
    public RewardDemoResponse markLatestCashbackPaid(UUID userId) {
        demoRepository.requireLocalDatabase();
        UUID commitmentId;
        try {
            commitmentId = demoRepository.latestCommitment(userId, "PAYABLE");
        } catch (IllegalStateException exception) {
            commitmentId = demoRepository.latestCommitment(userId, "PROCESSING");
        }
        demoRepository.markPaid(commitmentId, "DEMO-PAYOUT-" + commitmentId.toString().substring(0, 8).toUpperCase());
        return response(
                "MARK_CASHBACK_PAID",
                "The local Founder cashback was marked as returned to the original payment method.",
                userId
        );
    }

    public RewardDemoResponse reset(UUID currentUserId, String confirmation) {
        if (!RESET_CONFIRMATION.equals(confirmation)) {
            throw new IllegalArgumentException("Local reward reset confirmation did not match");
        }
        demoRepository.requireLocalDatabase();
        UUID programId = demoRepository.ensureProgram();
        int removedOrders = demoRepository.resetProgramData(programId, currentUserId);
        return response(
                "RESET_LOCAL_REWARD_DEMO",
                "Local demo data and this account's order history were cleared. Featured system test limits restored to 8 / 8 / 2 / 2. Removed " +
                        removedOrders + " Sales Orders.",
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

    private OffsetDateTime addBusinessDays(OffsetDateTime start, int days) {
        OffsetDateTime result = start;
        int added = 0;
        while (added < days) {
            result = result.plusDays(1);
            if (result.getDayOfWeek() != DayOfWeek.SATURDAY && result.getDayOfWeek() != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return result;
    }
}
