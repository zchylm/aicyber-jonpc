package com.aicyber.backend.reward.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RewardEventProcessorIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired RewardEventProcessor processor;
    private RewardDatabaseTestFixture fixture;

    @AfterEach void cleanUp() { if (fixture != null) fixture.cleanUp(); }

    @Test
    void locksTransparentCashbackWithoutCreatingFutureOrderAllocations() {
        fixture = new RewardDatabaseTestFixture(jdbcTemplate, 1);
        UUID orderId = fixture.createOrder(234_700);
        UUID eventId = fixture.createEvent(orderId);

        assertTrue(processor.processNext(fixture.programId()));
        Map<String, Object> commitment = jdbcTemplate.queryForMap(
                "SELECT founder_sequence, eligible_spend_cents, rate_basis_points, cap_cents, cashback_amount_cents, status " +
                        "FROM reward_commitments WHERE order_id = ?", orderId);

        assertEquals(1, commitment.get("founder_sequence"));
        assertEquals(213_363L, commitment.get("eligible_spend_cents"));
        assertEquals(1500, commitment.get("rate_basis_points"));
        assertEquals(50_000L, commitment.get("cap_cents"));
        assertEquals(32_004L, commitment.get("cashback_amount_cents"));
        assertEquals("LOCKED", commitment.get("status"));
        assertEquals("COMPLETED", jdbcTemplate.queryForObject("SELECT status FROM reward_inbox_events WHERE id = ?", String.class, eventId));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reward_contributions WHERE program_id = ?", Long.class, fixture.programId()));
        assertEquals(0L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reward_allocations", Long.class));
        assertFalse(processor.processNext(fixture.programId()));
    }

    @Test
    void appliesPublishedTierAndCapSnapshots() {
        fixture = new RewardDatabaseTestFixture(jdbcTemplate, 11);
        UUID orderId = fixture.createOrder(1_000_000);
        fixture.createEvent(orderId);

        assertTrue(processor.processNext(fixture.programId()));
        assertEquals(50_000L, jdbcTemplate.queryForObject(
                "SELECT cashback_amount_cents FROM reward_commitments WHERE order_id = ?", Long.class, orderId));
        assertEquals(1200, jdbcTemplate.queryForObject(
                "SELECT rate_basis_points FROM reward_commitments WHERE order_id = ?", Integer.class, orderId));
    }
}
