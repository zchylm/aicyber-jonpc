package com.aicyber.backend.reward.service;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

final class RewardDatabaseTestFixture {
    private final JdbcTemplate jdbcTemplate;
    private final UUID userId = UUID.randomUUID();
    private final UUID programId = UUID.randomUUID();

    RewardDatabaseTestFixture(JdbcTemplate jdbcTemplate, long nextQueueSequence) {
        this.jdbcTemplate = jdbcTemplate;
        UUID policyId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
                userId, userId + "@example.com", "test-password-hash", "Committed Reward Test User"
        );
        jdbcTemplate.update(
                "INSERT INTO reward_programs (id, code, name, next_queue_sequence) VALUES (?, ?, ?, ?)",
                programId, "TEST-" + programId, "Committed Test Reward Program", nextQueueSequence
        );
        jdbcTemplate.update(
                "INSERT INTO reward_policy_versions " +
                        "(id, program_id, version, calculation_type, rate_basis_points, status, effective_from) " +
                        "VALUES (?, ?, 1, 'ORDER_TOTAL_PERCENT', 2500, 'ACTIVE', ?)",
                policyId, programId, OffsetDateTime.now().minusMinutes(1)
        );
    }

    UUID programId() {
        return programId;
    }

    UUID createOrder(long amountCents) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO sales_orders " +
                        "(id, user_id, order_reference, amount_cents, status, paid_at, reward_eligible_at) " +
                        "VALUES (?, ?, ?, ?, 'REWARD_ELIGIBLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                orderId, userId, "T-" + orderId, amountCents
        );
        return orderId;
    }

    UUID createQueueEntry(UUID orderId, long sequence, long targetAmountCents) {
        UUID entryId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO reward_queue_entries " +
                        "(id, program_id, order_id, queue_sequence, target_amount_cents) VALUES (?, ?, ?, ?, ?)",
                entryId, programId, orderId, sequence, targetAmountCents
        );
        return entryId;
    }

    UUID createEvent(UUID orderId) {
        UUID eventId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO reward_inbox_events " +
                        "(id, program_id, external_event_id, event_type, order_id) " +
                        "VALUES (?, ?, ?, 'ORDER_REWARD_ELIGIBLE', ?)",
                eventId, programId, "EVENT-" + eventId, orderId
        );
        return eventId;
    }

    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM reward_allocations WHERE contribution_id IN " +
                        "(SELECT id FROM reward_contributions WHERE program_id = ?) " +
                        "OR recipient_queue_entry_id IN " +
                        "(SELECT id FROM reward_queue_entries WHERE program_id = ?)",
                programId, programId
        );
        jdbcTemplate.update("DELETE FROM reward_contributions WHERE program_id = ?", programId);
        jdbcTemplate.update("DELETE FROM reward_inbox_events WHERE program_id = ?", programId);
        jdbcTemplate.update("DELETE FROM reward_queue_entries WHERE program_id = ?", programId);
        jdbcTemplate.update("DELETE FROM reward_policy_versions WHERE program_id = ?", programId);
        jdbcTemplate.update("DELETE FROM reward_programs WHERE id = ?", programId);
        jdbcTemplate.update("DELETE FROM sales_orders WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
    }
}
