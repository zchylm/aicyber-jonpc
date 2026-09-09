package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.dto.RewardDemoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class RewardDemoServiceIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RewardDemoService demoService;

    @Test
    void convertsBuildRequestThenUsesIncomingOrderToAllocateRealContribution() {
        isolateDemoProgram();
        UUID userId = createUserWithBuildRequest();

        RewardDemoResponse joined = demoService.qualifyLatestBuildRequest(userId);

        assertEquals("ACTIVE", joined.member().state());
        assertEquals(1, joined.member().entries().size());
        assertEquals(200_000, joined.member().entries().get(0).targetAmountCents());
        assertEquals(0, joined.member().entries().get(0).allocatedAmountCents());
        assertEquals(1, joined.member().entries().get(0).currentPosition());

        RewardDemoResponse allocated = demoService.simulateIncomingOrder(userId);

        assertEquals(50_000, allocated.member().entries().get(0).allocatedAmountCents());
        assertEquals(150_000, allocated.member().entries().get(0).remainingAmountCents());
        assertEquals(25.0, allocated.member().entries().get(0).progressPercent());
        assertEquals(50_000, allocated.member().entries().get(0).latestAllocationAmountCents());
        assertEquals(2, allocated.summary().waitingCount());
        assertEquals(50_000, allocated.summary().totalAllocatedCents());
        assertTrue(allocated.summary().available());
        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_orders WHERE status = 'REWARD_ELIGIBLE' " +
                        "AND id IN (SELECT order_id FROM reward_queue_entries WHERE program_id = " +
                        "(SELECT id FROM reward_programs WHERE code = 'JON_QUEUE_REWARDS'))",
                Long.class
        ));
    }

    @Test
    void resetsOnlyLocalRewardDemoDataAndKeepsTheCustomerRequest() {
        isolateDemoProgram();
        UUID userId = createUserWithBuildRequest();

        demoService.qualifyLatestBuildRequest(userId);
        demoService.simulateIncomingOrder(userId);

        RewardDemoResponse reset = demoService.reset(userId, "RESET_LOCAL_REWARD_DEMO");

        assertEquals(0, reset.summary().waitingCount());
        assertEquals(0, reset.summary().completedCount());
        assertEquals(0, reset.summary().totalAllocatedCents());
        assertEquals("NO_ENTRY", reset.member().state());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Long.class,
                userId
        ));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM build_requests WHERE user_id = ?",
                Long.class,
                userId
        ));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_orders WHERE user_id = ?",
                Long.class,
                userId
        ));
    }

    private void isolateDemoProgram() {
        jdbcTemplate.query(
                "SELECT id FROM reward_programs WHERE code = 'JON_QUEUE_REWARDS'",
                resultSet -> {
                    if (!resultSet.next()) return null;
                    UUID programId = resultSet.getObject("id", UUID.class);
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
                    return null;
                }
        );
    }

    private UUID createUserWithBuildRequest() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
                userId, userId + "@example.com", "test-password-hash", "Reward Demo Test User"
        );
        jdbcTemplate.update(
                "INSERT INTO build_requests " +
                        "(id, user_id, request_reference, name, email, location, direction, estimated_price, " +
                        "recommended_baseline, configuration_snapshot) " +
                        "VALUES (?, ?, ?, 'Demo User', ?, 'Melbourne', 'gaming', 2000, 2000, '{}'::jsonb)",
                requestId,
                userId,
                "REQ-" + requestId,
                userId + "@example.com"
        );
        return userId;
    }
}
