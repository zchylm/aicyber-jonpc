package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.model.QueueEntryBalance;
import com.aicyber.backend.reward.model.RewardAllocation;
import com.aicyber.backend.reward.model.RewardAllocationPlan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Component;

@Component
public class RewardAllocationEngine {

    public RewardAllocationPlan allocate(long contributionAmountCents, List<QueueEntryBalance> queueEntries) {
        if (contributionAmountCents <= 0) {
            throw new IllegalArgumentException("contributionAmountCents must be positive");
        }
        Objects.requireNonNull(queueEntries, "queueEntries are required");

        List<QueueEntryBalance> orderedEntries = queueEntries.stream()
                .peek(Objects::requireNonNull)
                .sorted(Comparator.comparingLong(QueueEntryBalance::queueSequence))
                .toList();
        validateUniqueEntries(orderedEntries);

        long remainingContribution = contributionAmountCents;
        List<RewardAllocation> allocations = new ArrayList<>();

        for (QueueEntryBalance entry : orderedEntries) {
            if (remainingContribution == 0) {
                break;
            }

            long entryRemaining = entry.remainingAmountCents();
            if (entryRemaining == 0) {
                continue;
            }

            long allocationAmount = Math.min(remainingContribution, entryRemaining);
            allocations.add(new RewardAllocation(entry.queueEntryId(), allocationAmount));
            remainingContribution -= allocationAmount;
        }

        return new RewardAllocationPlan(allocations, remainingContribution);
    }

    private void validateUniqueEntries(List<QueueEntryBalance> queueEntries) {
        Set<UUID> entryIds = new HashSet<>();
        Set<Long> sequences = new HashSet<>();

        for (QueueEntryBalance entry : queueEntries) {
            if (!entryIds.add(entry.queueEntryId())) {
                throw new IllegalArgumentException("queue entry ids must be unique");
            }
            if (!sequences.add(entry.queueSequence())) {
                throw new IllegalArgumentException("queue sequences must be unique");
            }
        }
    }
}
