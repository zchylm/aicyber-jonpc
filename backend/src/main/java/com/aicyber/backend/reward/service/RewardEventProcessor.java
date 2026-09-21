package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.model.EligibleSalesOrder;
import com.aicyber.backend.reward.model.FounderTier;
import com.aicyber.backend.reward.model.PendingRewardEvent;
import com.aicyber.backend.reward.model.RewardProgramState;
import com.aicyber.backend.reward.repository.RewardProcessingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class RewardEventProcessor {
    private final RewardProcessingRepository repository;
    public RewardEventProcessor(RewardProcessingRepository repository) {
        this.repository = repository;
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
        UUID userId = repository.findOrderUser(order.id());
        long founderSequence = program.nextQueueSequence();
        if (founderSequence <= program.maxPositions() && !repository.commitmentExists(programId, order.id(), userId)) {
            FounderTier tier = repository.findTier(programId, founderSequence)
                    .orElseThrow(() -> new IllegalStateException("Founder tier was not configured for this position"));
            long eligibleSpendCents = order.amountCents() * 10 / 11;
            long calculatedCashbackCents = eligibleSpendCents * tier.rateBasisPoints() / 10_000;
            long cashbackAmountCents = Math.min(calculatedCashbackCents, tier.capCents());
            long committedAfter = repository.committedLiability(programId) + cashbackAmountCents;
            if (committedAfter > program.maxLiabilityCents()) {
                throw new IllegalStateException("Founder campaign liability limit would be exceeded");
            }
            repository.advanceQueueSequence(programId, founderSequence);
            repository.createCommitment(
                    programId, tier.id(), order.id(), userId, founderSequence, order.amountCents(), eligibleSpendCents,
                    tier.rateBasisPoints(), tier.capCents(), cashbackAmountCents
            );
        }
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
