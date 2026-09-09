package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.model.QueueEntryBalance;
import com.aicyber.backend.reward.model.RewardAllocationPlan;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RewardAllocationEngineTest {

    private final RewardAllocationEngine engine = new RewardAllocationEngine();

    @Test
    void splitsContributionAcrossQueueEntriesInFifoOrder() {
        UUID firstEntryId = UUID.randomUUID();
        UUID secondEntryId = UUID.randomUUID();

        RewardAllocationPlan plan = engine.allocate(50_000, List.of(
                entry(secondEntryId, 1002, 150_000, 0),
                entry(firstEntryId, 1001, 200_000, 180_000)
        ));

        assertEquals(2, plan.allocations().size());
        assertEquals(firstEntryId, plan.allocations().get(0).queueEntryId());
        assertEquals(20_000, plan.allocations().get(0).amountCents());
        assertEquals(secondEntryId, plan.allocations().get(1).queueEntryId());
        assertEquals(30_000, plan.allocations().get(1).amountCents());
        assertEquals(0, plan.remainingContributionCents());
    }

    @Test
    void keepsContributionRemainderWhenTheQueueCannotAbsorbIt() {
        UUID entryId = UUID.randomUUID();

        RewardAllocationPlan plan = engine.allocate(50_000, List.of(
                entry(entryId, 1001, 200_000, 190_000)
        ));

        assertEquals(1, plan.allocations().size());
        assertEquals(10_000, plan.allocations().get(0).amountCents());
        assertEquals(40_000, plan.remainingContributionCents());
    }

    @Test
    void skipsEntriesThatHaveAlreadyReachedTheirTarget() {
        UUID completedEntryId = UUID.randomUUID();
        UUID waitingEntryId = UUID.randomUUID();

        RewardAllocationPlan plan = engine.allocate(25_000, List.of(
                entry(completedEntryId, 1001, 200_000, 200_000),
                entry(waitingEntryId, 1002, 150_000, 0)
        ));

        assertEquals(1, plan.allocations().size());
        assertEquals(waitingEntryId, plan.allocations().get(0).queueEntryId());
        assertEquals(25_000, plan.allocations().get(0).amountCents());
    }

    @Test
    void leavesTheWholeContributionWhenThereAreNoRecipients() {
        RewardAllocationPlan plan = engine.allocate(50_000, List.of());

        assertEquals(List.of(), plan.allocations());
        assertEquals(50_000, plan.remainingContributionCents());
    }

    @Test
    void rejectsNonPositiveContributions() {
        assertThrows(IllegalArgumentException.class, () -> engine.allocate(0, List.of()));
        assertThrows(IllegalArgumentException.class, () -> engine.allocate(-1, List.of()));
    }

    @Test
    void rejectsDuplicateQueueEntriesAndSequences() {
        UUID entryId = UUID.randomUUID();

        assertThrows(IllegalArgumentException.class, () -> engine.allocate(50_000, List.of(
                entry(entryId, 1001, 200_000, 0),
                entry(entryId, 1002, 200_000, 0)
        )));
        assertThrows(IllegalArgumentException.class, () -> engine.allocate(50_000, List.of(
                entry(UUID.randomUUID(), 1001, 200_000, 0),
                entry(UUID.randomUUID(), 1001, 200_000, 0)
        )));
    }

    @Test
    void rejectsAnAllocatedAmountAboveTheTarget() {
        assertThrows(IllegalArgumentException.class,
                () -> entry(UUID.randomUUID(), 1001, 200_000, 200_001));
    }

    private QueueEntryBalance entry(
            UUID id,
            long sequence,
            long targetAmountCents,
            long allocatedAmountCents
    ) {
        return new QueueEntryBalance(id, sequence, targetAmountCents, allocatedAmountCents);
    }
}
