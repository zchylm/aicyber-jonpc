package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.model.QueueEntryBalance;
import com.aicyber.backend.reward.model.RewardAllocationPlan;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(RewardEventProcessorRollbackIntegrationTest.FailingEngineConfiguration.class)
class RewardEventProcessorRollbackIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RewardEventProcessor processor;

    private RewardDatabaseTestFixture fixture;

    @AfterEach
    void cleanUp() {
        if (fixture != null) {
            fixture.cleanUp();
        }
    }

    @Test
    void rollsBackEveryDatabaseChangeWhenAllocationFails() {
        fixture = new RewardDatabaseTestFixture(jdbcTemplate, 1);
        UUID orderId = fixture.createOrder(200_000);
        UUID eventId = fixture.createEvent(orderId);

        assertThrows(IllegalStateException.class, () -> processor.processNext(fixture.programId()));

        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT next_queue_sequence FROM reward_programs WHERE id = ?",
                Long.class,
                fixture.programId()
        ));
        assertEquals(0L, count("reward_queue_entries"));
        assertEquals(0L, count("reward_contributions"));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_allocations a " +
                        "JOIN reward_contributions c ON c.id = a.contribution_id " +
                        "WHERE c.program_id = ?",
                Long.class,
                fixture.programId()
        ));
        assertEquals("PENDING", jdbcTemplate.queryForObject(
                "SELECT status FROM reward_inbox_events WHERE id = ?", String.class, eventId));
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE program_id = ?",
                Long.class,
                fixture.programId()
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingEngineConfiguration {
        @Bean
        @Primary
        RewardAllocationEngine failingRewardAllocationEngine() {
            return new RewardAllocationEngine() {
                @Override
                public RewardAllocationPlan allocate(
                        long contributionAmountCents,
                        List<QueueEntryBalance> queueEntries
                ) {
                    throw new IllegalStateException("Deliberate allocation failure");
                }
            };
        }
    }
}
