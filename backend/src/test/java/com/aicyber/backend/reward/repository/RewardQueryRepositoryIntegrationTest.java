package com.aicyber.backend.reward.repository;

import com.aicyber.backend.reward.service.RewardEventProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class RewardQueryRepositoryIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired RewardQueryRepository repository;
    @Autowired RewardEventProcessor processor;

    @Test
    void returnsOnlyCommitmentsOwnedByTheRequestedUser() {
        UUID programId = createProgram();
        UUID firstUser = createUser();
        UUID secondUser = createUser();
        createEligibleOrderAndProcess(programId, firstUser);
        createEligibleOrderAndProcess(programId, secondUser);

        assertEquals(1, repository.findEntries(programId, firstUser).size());
        assertEquals(1, repository.findEntries(programId, secondUser).size());
        assertEquals(1, repository.findEntries(programId, firstUser).get(0).founderNumber());
        assertEquals(2, repository.findEntries(programId, secondUser).get(0).founderNumber());
    }

    private UUID createProgram() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO reward_programs (id, code, name) VALUES (?, ?, 'Privacy Test')", id, "PRIVACY-" + id);
        for (Object[] tier : new Object[][] {{"L", "Launch", 1, 10, 1500, 50000}, {"E", "Early", 11, 25, 1200, 50000}, {"F", "Founder", 26, 50, 1000, 50000}}) {
            jdbcTemplate.update("INSERT INTO reward_founder_tiers (id, program_id, tier_code, display_name, position_start, position_end, rate_basis_points, cap_cents) VALUES (?, ?, ?, ?, ?, ?, ?, ?)", UUID.randomUUID(), id, tier[0], tier[1], tier[2], tier[3], tier[4], tier[5]);
        }
        return id;
    }

    private UUID createUser() {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'hash', 'Customer')", id, id + "@example.com");
        return id;
    }

    private void createEligibleOrderAndProcess(UUID programId, UUID userId) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO sales_orders (id, user_id, order_reference, amount_cents, status, paid_at, reward_eligible_at) VALUES (?, ?, ?, 200000, 'REWARD_ELIGIBLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", orderId, userId, "SO-" + orderId);
        jdbcTemplate.update("INSERT INTO reward_inbox_events (id, program_id, external_event_id, event_type, order_id) VALUES (?, ?, ?, 'ORDER_REWARD_ELIGIBLE', ?)", UUID.randomUUID(), programId, "EVENT-" + orderId, orderId);
        processor.processNext(programId);
    }
}
