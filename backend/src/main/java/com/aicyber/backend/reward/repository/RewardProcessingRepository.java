package com.aicyber.backend.reward.repository;

import com.aicyber.backend.reward.model.EligibleSalesOrder;
import com.aicyber.backend.reward.model.FounderTier;
import com.aicyber.backend.reward.model.PendingRewardEvent;
import com.aicyber.backend.reward.model.RewardProgramState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
                "SELECT id, currency, status, next_founder_sequence, max_positions, max_liability_cents " +
                        "FROM reward_programs WHERE id = ? FOR UPDATE",
                (resultSet, rowNumber) -> new RewardProgramState(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("currency").trim(),
                        resultSet.getString("status"),
                        resultSet.getLong("next_founder_sequence"),
                        resultSet.getInt("max_positions"),
                        resultSet.getLong("max_liability_cents")
                ),
                programId
        ).stream().findFirst();
    }

    public Optional<FounderTier> findTier(UUID programId, long founderSequence) {
        return jdbcTemplate.query(
                "SELECT id, tier_code, display_name, rate_basis_points, cap_cents " +
                        "FROM reward_founder_tiers WHERE program_id = ? AND ? BETWEEN position_start AND position_end",
                (resultSet, rowNumber) -> new FounderTier(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("tier_code"),
                        resultSet.getString("display_name"),
                        resultSet.getInt("rate_basis_points"),
                        resultSet.getLong("cap_cents")
                ),
                programId,
                founderSequence
        ).stream().findFirst();
    }

    public boolean commitmentExists(UUID programId, UUID orderId, UUID userId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_commitments WHERE program_id = ? AND (order_id = ? OR user_id = ?)",
                Long.class,
                programId,
                orderId,
                userId
        );
        return count != null && count > 0;
    }

    public UUID findOrderUser(UUID orderId) {
        return jdbcTemplate.queryForObject("SELECT user_id FROM sales_orders WHERE id = ?", UUID.class, orderId);
    }

    public long committedLiability(UUID programId) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(cashback_amount_cents), 0) FROM reward_commitments " +
                        "WHERE program_id = ? AND status <> 'VOID'",
                Long.class,
                programId
        );
        return total == null ? 0 : total;
    }

    public void createCommitment(
            UUID programId,
            UUID tierId,
            UUID orderId,
            UUID userId,
            long founderSequence,
            long purchaseAmountCents,
            long eligibleSpendCents,
            int rateBasisPoints,
            long capCents,
            long cashbackAmountCents
    ) {
        jdbcTemplate.update(
                "INSERT INTO reward_commitments " +
                        "(id, program_id, tier_id, order_id, user_id, founder_sequence, purchase_amount_cents, " +
                        "eligible_spend_cents, rate_basis_points, cap_cents, cashback_amount_cents) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), programId, tierId, orderId, userId, founderSequence, purchaseAmountCents,
                eligibleSpendCents, rateBasisPoints, capCents, cashbackAmountCents
        );
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

    public void advanceQueueSequence(UUID programId, long expectedSequence) {
        int updated = jdbcTemplate.update(
                "UPDATE reward_programs SET next_founder_sequence = next_founder_sequence + 1, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND next_founder_sequence = ?",
                programId,
                expectedSequence
        );
        requireSingleUpdate(updated, "Reward program queue sequence changed unexpectedly");
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
