package com.aicyber.backend.reward.service;

import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

final class RewardDatabaseTestFixture {
    private final JdbcTemplate jdbcTemplate;
    private final List<UUID> userIds = new ArrayList<>();
    private final UUID programId = UUID.randomUUID();

    RewardDatabaseTestFixture(JdbcTemplate jdbcTemplate, long nextQueueSequence) {
        this.jdbcTemplate = jdbcTemplate;
        jdbcTemplate.update(
                "INSERT INTO reward_programs (id, code, name, next_founder_sequence) VALUES (?, ?, ?, ?)",
                programId, "TEST-" + programId, "Committed Test Reward Program", nextQueueSequence
        );
        insertTier("LAUNCH", "Launch Founder", 1, 10, 1500, 50_000);
        insertTier("EARLY", "Early Founder", 11, 25, 1200, 50_000);
        insertTier("FOUNDER", "Founder", 26, 50, 1000, 50_000);
    }

    UUID programId() {
        return programId;
    }

    UUID createOrder(long amountCents) {
        UUID userId = UUID.randomUUID();
        userIds.add(userId);
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
                userId, userId + "@example.com", "test-password-hash", "Founder Test User"
        );
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO sales_orders " +
                        "(id, user_id, order_reference, amount_cents, status, paid_at, reward_eligible_at) " +
                        "VALUES (?, ?, ?, ?, 'REWARD_ELIGIBLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                orderId, userId, "T-" + orderId, amountCents
        );
        return orderId;
    }

    private void insertTier(String code, String name, int start, int end, int rate, long cap) {
        jdbcTemplate.update(
                "INSERT INTO reward_founder_tiers " +
                        "(id, program_id, tier_code, display_name, position_start, position_end, rate_basis_points, cap_cents) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), programId, code, name, start, end, rate, cap
        );
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
        jdbcTemplate.update("DELETE FROM reward_commitments WHERE program_id = ?", programId);
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
        jdbcTemplate.update("DELETE FROM reward_founder_tiers WHERE program_id = ?", programId);
        jdbcTemplate.update("DELETE FROM reward_programs WHERE id = ?", programId);
        for (UUID userId : userIds) {
            jdbcTemplate.update("DELETE FROM sales_orders WHERE user_id = ?", userId);
            jdbcTemplate.update("DELETE FROM users WHERE id = ?", userId);
        }
    }
}
