package com.aicyber.backend.reward.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class RewardEventProcessorRollbackIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired RewardEventProcessor processor;
    private RewardDatabaseTestFixture fixture;

    @AfterEach void cleanUp() { if (fixture != null) fixture.cleanUp(); }

    @Test
    void rollsBackSequenceAndCommitmentWhenLiabilityGuardFails() {
        fixture = new RewardDatabaseTestFixture(jdbcTemplate, 1);
        jdbcTemplate.update("UPDATE reward_programs SET max_liability_cents = 1 WHERE id = ?", fixture.programId());
        UUID eventId = fixture.createEvent(fixture.createOrder(200_000));

        assertThrows(IllegalStateException.class, () -> processor.processNext(fixture.programId()));

        assertEquals(1L, jdbcTemplate.queryForObject("SELECT next_founder_sequence FROM reward_programs WHERE id = ?", Long.class, fixture.programId()));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reward_commitments WHERE program_id = ?", Long.class, fixture.programId()));
        assertEquals("PENDING", jdbcTemplate.queryForObject("SELECT status FROM reward_inbox_events WHERE id = ?", String.class, eventId));
    }
}
