package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.model.EligibleSalesOrder;
import com.aicyber.backend.reward.model.PendingRewardEvent;
import com.aicyber.backend.reward.model.QueueEntryBalance;
import com.aicyber.backend.reward.model.RewardAllocation;
import com.aicyber.backend.reward.model.RewardAllocationPlan;
import com.aicyber.backend.reward.model.RewardPolicy;
import com.aicyber.backend.reward.model.RewardProgramState;
import com.aicyber.backend.reward.repository.RewardProcessingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RewardEventProcessor {
    private final RewardProcessingRepository repository;
    private final ContributionCalculator contributionCalculator;
    private final RewardAllocationEngine allocationEngine;

    public RewardEventProcessor(
            RewardProcessingRepository repository,
            ContributionCalculator contributionCalculator,
            RewardAllocationEngine allocationEngine
    ) {
        this.repository = repository;
        this.contributionCalculator = contributionCalculator;
        this.allocationEngine = allocationEngine;
    }

    @Transactional
    public boolean processNext(UUID programId) {
        RewardProgramState program = repository.lockProgram(programId)
                .orElseThrow(() -> new IllegalArgumentException("Reward program was not found"));
        if (!"ACTIVE".equals(program.status())) {
            return false;
        }

        Optional<PendingRewardEvent> pendingEvent = repository.lockNextPendingEvent(programId);
        if (pendingEvent.isEmpty()) {
            return false;
        }

        PendingRewardEvent event = pendingEvent.get();
        EligibleSalesOrder order = repository.findOrder(event.orderId());
        validateEligibleOrder(program, order);

        RewardPolicy policy = repository.findActivePolicy(programId);
        long contributionAmountCents = contributionCalculator.calculate(order.amountCents(), policy);
        long queueSequence = program.nextQueueSequence();

        repository.advanceQueueSequence(programId, queueSequence);
        repository.createQueueEntry(programId, order.id(), queueSequence, order.amountCents());
        UUID contributionId = repository.createContribution(
                programId,
                order.id(),
                policy.id(),
                contributionAmountCents
        );

        List<QueueEntryBalance> recipients = repository.lockWaitingEntriesBefore(programId, queueSequence);
        RewardAllocationPlan plan = allocationEngine.allocate(contributionAmountCents, recipients);

        for (RewardAllocation allocation : plan.allocations()) {
            repository.applyAllocation(contributionId, allocation);
        }
        repository.updateContribution(
                contributionId,
                contributionAmountCents,
                plan.remainingContributionCents()
        );
        repository.completeEvent(event.id());
        return true;
    }

    private void validateEligibleOrder(RewardProgramState program, EligibleSalesOrder order) {
        if (!"REWARD_ELIGIBLE".equals(order.status())) {
            throw new IllegalStateException("Order is not eligible for rewards");
        }
        if (!program.currency().equals(order.currency())) {
            throw new IllegalStateException("Order and reward program currencies do not match");
        }
    }
}
