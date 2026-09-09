package com.aicyber.backend.reward.repository;

import com.aicyber.backend.reward.model.EligibleSalesOrder;
import com.aicyber.backend.reward.model.PendingRewardEvent;
import com.aicyber.backend.reward.model.QueueEntryBalance;
import com.aicyber.backend.reward.model.RewardAllocation;
import com.aicyber.backend.reward.model.RewardPolicy;
import com.aicyber.backend.reward.model.RewardProgramState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RewardProcessingRepository {
    private final JdbcTemplate jdbcTemplate;

    public RewardProcessingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<RewardProgramState> lockProgram(UUID programId) {
        return jdbcTemplate.query(
                "SELECT id, currency, status, next_queue_sequence FROM reward_programs WHERE id = ? FOR UPDATE",
                (resultSet, rowNumber) -> new RewardProgramState(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("currency").trim(),
                        resultSet.getString("status"),
                        resultSet.getLong("next_queue_sequence")
                ),
                programId
        ).stream().findFirst();
    }

    public Optional<PendingRewardEvent> lockNextPendingEvent(UUID programId) {
        return jdbcTemplate.query(
                "SELECT id, order_id FROM reward_inbox_events " +
                        "WHERE program_id = ? AND status = 'PENDING' " +
                        "ORDER BY event_sequence LIMIT 1 FOR UPDATE",
                (resultSet, rowNumber) -> new PendingRewardEvent(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getObject("order_id", UUID.class)
                ),
                programId
        ).stream().findFirst();
    }

    public EligibleSalesOrder findOrder(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT id, amount_cents, currency, status FROM sales_orders WHERE id = ?",
                (resultSet, rowNumber) -> new EligibleSalesOrder(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getLong("amount_cents"),
                        resultSet.getString("currency").trim(),
                        resultSet.getString("status")
                ),
                orderId
        );
    }

    public RewardPolicy findActivePolicy(UUID programId) {
        return jdbcTemplate.queryForObject(
                "SELECT id, calculation_type, rate_basis_points, fixed_amount_cents " +
                        "FROM reward_policy_versions WHERE program_id = ? AND status = 'ACTIVE' " +
                        "AND effective_from <= CURRENT_TIMESTAMP " +
                        "AND (effective_to IS NULL OR effective_to > CURRENT_TIMESTAMP)",
                (resultSet, rowNumber) -> new RewardPolicy(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("calculation_type"),
                        (Integer) resultSet.getObject("rate_basis_points"),
                        (Long) resultSet.getObject("fixed_amount_cents")
                ),
                programId
        );
    }

    public void advanceQueueSequence(UUID programId, long expectedSequence) {
        int updated = jdbcTemplate.update(
                "UPDATE reward_programs SET next_queue_sequence = next_queue_sequence + 1, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND next_queue_sequence = ?",
                programId,
                expectedSequence
        );
        requireSingleUpdate(updated, "Reward program queue sequence changed unexpectedly");
    }

    public UUID createQueueEntry(UUID programId, UUID orderId, long queueSequence, long targetAmountCents) {
        UUID queueEntryId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO reward_queue_entries " +
                        "(id, program_id, order_id, queue_sequence, target_amount_cents) VALUES (?, ?, ?, ?, ?)",
                queueEntryId,
                programId,
                orderId,
                queueSequence,
                targetAmountCents
        );
        return queueEntryId;
    }

    public UUID createContribution(
            UUID programId,
            UUID sourceOrderId,
            UUID policyVersionId,
            long amountCents
    ) {
        UUID contributionId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO reward_contributions " +
                        "(id, program_id, source_order_id, policy_version_id, original_amount_cents, remaining_amount_cents) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                contributionId,
                programId,
                sourceOrderId,
                policyVersionId,
                amountCents,
                amountCents
        );
        return contributionId;
    }

    public List<QueueEntryBalance> lockWaitingEntriesBefore(UUID programId, long sourceQueueSequence) {
        return jdbcTemplate.query(
                "SELECT id, queue_sequence, target_amount_cents, allocated_amount_cents " +
                        "FROM reward_queue_entries " +
                        "WHERE program_id = ? AND status = 'WAITING' AND queue_sequence < ? " +
                        "ORDER BY queue_sequence FOR UPDATE",
                (resultSet, rowNumber) -> new QueueEntryBalance(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getLong("queue_sequence"),
                        resultSet.getLong("target_amount_cents"),
                        resultSet.getLong("allocated_amount_cents")
                ),
                programId,
                sourceQueueSequence
        );
    }

    public void applyAllocation(UUID contributionId, RewardAllocation allocation) {
        jdbcTemplate.update(
                "INSERT INTO reward_allocations " +
                        "(id, contribution_id, recipient_queue_entry_id, amount_cents) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(),
                contributionId,
                allocation.queueEntryId(),
                allocation.amountCents()
        );

        int updated = jdbcTemplate.update(
                "UPDATE reward_queue_entries SET " +
                        "allocated_amount_cents = allocated_amount_cents + ?, " +
                        "status = CASE WHEN allocated_amount_cents + ? = target_amount_cents " +
                        "THEN 'COMPLETED' ELSE 'WAITING' END, " +
                        "completed_at = CASE WHEN allocated_amount_cents + ? = target_amount_cents " +
                        "THEN CURRENT_TIMESTAMP ELSE NULL END " +
                        "WHERE id = ? AND status = 'WAITING' " +
                        "AND allocated_amount_cents + ? <= target_amount_cents",
                allocation.amountCents(),
                allocation.amountCents(),
                allocation.amountCents(),
                allocation.queueEntryId(),
                allocation.amountCents()
        );
        requireSingleUpdate(updated, "Queue entry could not accept the allocation");
    }

    public void updateContribution(UUID contributionId, long originalAmountCents, long remainingAmountCents) {
        String status = remainingAmountCents == 0
                ? "ALLOCATED"
                : remainingAmountCents == originalAmountCents ? "PENDING" : "PARTIALLY_ALLOCATED";

        int updated = jdbcTemplate.update(
                "UPDATE reward_contributions SET remaining_amount_cents = ?, status = ?, " +
                        "fully_allocated_at = CASE WHEN ? = 0 THEN CURRENT_TIMESTAMP ELSE NULL END " +
                        "WHERE id = ? AND original_amount_cents = ?",
                remainingAmountCents,
                status,
                remainingAmountCents,
                contributionId,
                originalAmountCents
        );
        requireSingleUpdate(updated, "Contribution changed unexpectedly");
    }

    public void completeEvent(UUID eventId) {
        int updated = jdbcTemplate.update(
                "UPDATE reward_inbox_events SET status = 'COMPLETED', " +
                        "attempt_count = attempt_count + 1, processed_at = CURRENT_TIMESTAMP, last_error = NULL " +
                        "WHERE id = ? AND status = 'PENDING'",
                eventId
        );
        requireSingleUpdate(updated, "Reward event was not pending");
    }

    private void requireSingleUpdate(int updatedRows, String message) {
        if (updatedRows != 1) {
            throw new IllegalStateException(message);
        }
    }
}
