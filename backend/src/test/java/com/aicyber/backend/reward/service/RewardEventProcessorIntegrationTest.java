package com.aicyber.backend.reward.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class RewardEventProcessorIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RewardEventProcessor processor;

    @Test
    void connectsEligibleOrderToQueueContributionAndAllocationLedger() {
        TestData data = createProgramWithPolicy(1003);
        UUID firstOrderId = createOrder(data.userId(), 200_000);
        UUID secondOrderId = createOrder(data.userId(), 150_000);
        UUID sourceOrderId = createOrder(data.userId(), 200_000);
        UUID firstEntryId = createQueueEntry(data.programId(), firstOrderId, 1001, 200_000, 180_000);
        UUID secondEntryId = createQueueEntry(data.programId(), secondOrderId, 1002, 150_000, 0);
        UUID eventId = createEvent(data.programId(), sourceOrderId, "X");

        assertTrue(processor.processNext(data.programId()));

        assertQueueEntry(firstEntryId, 200_000, "COMPLETED");
        assertQueueEntry(secondEntryId, 30_000, "WAITING");
        assertQueueEntryForOrder(sourceOrderId, 1003, 200_000, 0);

        List<Map<String, Object>> allocations = jdbcTemplate.queryForList(
                "SELECT a.recipient_queue_entry_id, a.amount_cents " +
                        "FROM reward_allocations a " +
                        "JOIN reward_queue_entries q ON q.id = a.recipient_queue_entry_id " +
                        "JOIN reward_contributions c ON c.id = a.contribution_id " +
                        "WHERE c.source_order_id = ? ORDER BY q.queue_sequence",
                sourceOrderId
        );
        assertEquals(2, allocations.size());
        assertEquals(firstEntryId, allocations.get(0).get("recipient_queue_entry_id"));
        assertEquals(20_000L, allocations.get(0).get("amount_cents"));
        assertEquals(secondEntryId, allocations.get(1).get("recipient_queue_entry_id"));
        assertEquals(30_000L, allocations.get(1).get("amount_cents"));

        Map<String, Object> contribution = jdbcTemplate.queryForMap(
                "SELECT original_amount_cents, remaining_amount_cents, status " +
                        "FROM reward_contributions WHERE source_order_id = ?",
                sourceOrderId
        );
        assertEquals(50_000L, contribution.get("original_amount_cents"));
        assertEquals(0L, contribution.get("remaining_amount_cents"));
        assertEquals("ALLOCATED", contribution.get("status"));
        assertEquals("COMPLETED", jdbcTemplate.queryForObject(
                "SELECT status FROM reward_inbox_events WHERE id = ?", String.class, eventId));
        assertEquals(1004L, jdbcTemplate.queryForObject(
                "SELECT next_queue_sequence FROM reward_programs WHERE id = ?", Long.class, data.programId()));
        assertFalse(processor.processNext(data.programId()));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_contributions WHERE source_order_id = ?", Long.class, sourceOrderId));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_queue_entries WHERE order_id = ?", Long.class, sourceOrderId));
    }

    @Test
    void keepsContributionPendingWhenThereIsNoEarlierQueueEntry() {
        TestData data = createProgramWithPolicy(1);
        UUID sourceOrderId = createOrder(data.userId(), 200_000);
        createEvent(data.programId(), sourceOrderId, "FIRST");

        assertTrue(processor.processNext(data.programId()));

        assertQueueEntryForOrder(sourceOrderId, 1, 200_000, 0);
        Map<String, Object> contribution = jdbcTemplate.queryForMap(
                "SELECT original_amount_cents, remaining_amount_cents, status " +
                        "FROM reward_contributions WHERE source_order_id = ?",
                sourceOrderId
        );
        assertEquals(50_000L, contribution.get("original_amount_cents"));
        assertEquals(50_000L, contribution.get("remaining_amount_cents"));
        assertEquals("PENDING", contribution.get("status"));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_allocations a " +
                        "JOIN reward_contributions c ON c.id = a.contribution_id " +
                        "WHERE c.source_order_id = ?",
                Long.class,
                sourceOrderId
        ));
    }

    @Test
    void processesMultipleEventsInTheirDatabaseSequence() {
        TestData data = createProgramWithPolicy(1);
        UUID firstOrderId = createOrder(data.userId(), 200_000);
        UUID secondOrderId = createOrder(data.userId(), 200_000);
        UUID firstEventId = createEvent(data.programId(), firstOrderId, "FIRST");
        UUID secondEventId = createEvent(data.programId(), secondOrderId, "SECOND");

        assertTrue(processor.processNext(data.programId()));

        assertQueueEntryForOrder(firstOrderId, 1, 200_000, 0);
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_queue_entries WHERE order_id = ?", Long.class, secondOrderId));
        assertEquals("COMPLETED", eventStatus(firstEventId));
        assertEquals("PENDING", eventStatus(secondEventId));

        assertTrue(processor.processNext(data.programId()));

        assertQueueEntryForOrder(firstOrderId, 1, 200_000, 50_000);
        assertQueueEntryForOrder(secondOrderId, 2, 200_000, 0);
        assertEquals("COMPLETED", eventStatus(secondEventId));
    }

    private TestData createProgramWithPolicy(long nextQueueSequence) {
        UUID userId = UUID.randomUUID();
        UUID programId = UUID.randomUUID();
        UUID policyId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
                userId, userId + "@example.com", "test-password-hash", "Reward Test User"
        );
        jdbcTemplate.update(
                "INSERT INTO reward_programs (id, code, name, next_queue_sequence) VALUES (?, ?, ?, ?)",
                programId, "TEST-" + programId, "Test Reward Program", nextQueueSequence
        );
        jdbcTemplate.update(
                "INSERT INTO reward_policy_versions " +
                        "(id, program_id, version, calculation_type, rate_basis_points, status, effective_from) " +
                        "VALUES (?, ?, 1, 'ORDER_TOTAL_PERCENT', 2500, 'ACTIVE', ?)",
                policyId, programId, OffsetDateTime.now().minusMinutes(1)
        );
        return new TestData(userId, programId);
    }

    private UUID createOrder(UUID userId, long amountCents) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO sales_orders " +
                        "(id, user_id, order_reference, amount_cents, status, paid_at, reward_eligible_at) " +
                        "VALUES (?, ?, ?, ?, 'REWARD_ELIGIBLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                orderId, userId, "T-" + orderId, amountCents
        );
        return orderId;
    }

    private UUID createQueueEntry(
            UUID programId,
            UUID orderId,
            long sequence,
            long targetAmountCents,
            long allocatedAmountCents
    ) {
        UUID entryId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO reward_queue_entries " +
                        "(id, program_id, order_id, queue_sequence, target_amount_cents, allocated_amount_cents) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                entryId, programId, orderId, sequence, targetAmountCents, allocatedAmountCents
        );
        return entryId;
    }

    private UUID createEvent(UUID programId, UUID orderId, String suffix) {
        UUID eventId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO reward_inbox_events " +
                        "(id, program_id, external_event_id, event_type, order_id) " +
                        "VALUES (?, ?, ?, 'ORDER_REWARD_ELIGIBLE', ?)",
                eventId, programId, "EVENT-" + suffix + "-" + eventId, orderId
        );
        return eventId;
    }

    private void assertQueueEntry(UUID entryId, long allocatedAmountCents, String status) {
        Map<String, Object> entry = jdbcTemplate.queryForMap(
                "SELECT allocated_amount_cents, status FROM reward_queue_entries WHERE id = ?",
                entryId
        );
        assertEquals(allocatedAmountCents, entry.get("allocated_amount_cents"));
        assertEquals(status, entry.get("status"));
    }

    private void assertQueueEntryForOrder(
            UUID orderId,
            long sequence,
            long targetAmountCents,
            long allocatedAmountCents
    ) {
        Map<String, Object> entry = jdbcTemplate.queryForMap(
                "SELECT queue_sequence, target_amount_cents, allocated_amount_cents " +
                        "FROM reward_queue_entries WHERE order_id = ?",
                orderId
        );
        assertEquals(sequence, entry.get("queue_sequence"));
        assertEquals(targetAmountCents, entry.get("target_amount_cents"));
        assertEquals(allocatedAmountCents, entry.get("allocated_amount_cents"));
    }

    private String eventStatus(UUID eventId) {
        return jdbcTemplate.queryForObject(
                "SELECT status FROM reward_inbox_events WHERE id = ?", String.class, eventId);
    }

    private record TestData(UUID userId, UUID programId) {
    }
}
