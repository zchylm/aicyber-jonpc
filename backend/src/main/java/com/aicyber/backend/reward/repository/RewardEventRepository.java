package com.aicyber.backend.reward.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class RewardEventRepository {
    private final JdbcTemplate jdbcTemplate;

    public RewardEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean createOrderEligibleEvent(UUID programId, UUID orderId) {
        int inserted = jdbcTemplate.update(
                "INSERT INTO reward_inbox_events " +
                        "(id, program_id, external_event_id, event_type, order_id) " +
                        "VALUES (?, ?, ?, 'ORDER_REWARD_ELIGIBLE', ?) " +
                        "ON CONFLICT (program_id, order_id, event_type) DO NOTHING",
                UUID.randomUUID(),
                programId,
                "ORDER_REWARD_ELIGIBLE:" + orderId,
                orderId
        );
        return inserted == 1;
    }
}
