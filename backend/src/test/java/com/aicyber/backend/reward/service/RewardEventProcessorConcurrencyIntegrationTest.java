package com.aicyber.backend.reward.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RewardEventProcessorConcurrencyIntegrationTest {

    private static final int CONCURRENT_ORDERS = 100;
    private static final long ORDER_AMOUNT_CENTS = 200_000;
    private static final long CONTRIBUTION_AMOUNT_CENTS = 50_000;

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
    @Timeout(90)
    void serializesOneHundredSimultaneousEventsWithoutDuplicatesOrOverAllocation() throws Exception {
        fixture = new RewardDatabaseTestFixture(jdbcTemplate, 2);
        UUID headOrderId = fixture.createOrder(10_000_000);
        UUID headEntryId = fixture.createQueueEntry(headOrderId, 1, 10_000_000);

        for (int index = 0; index < CONCURRENT_ORDERS; index++) {
            fixture.createEvent(fixture.createOrder(ORDER_AMOUNT_CENTS));
        }

        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_ORDERS);
        CountDownLatch readyGate = new CountDownLatch(CONCURRENT_ORDERS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int index = 0; index < CONCURRENT_ORDERS; index++) {
                results.add(executor.submit(() -> {
                    readyGate.countDown();
                    startGate.await();
                    return processor.processNext(fixture.programId());
                }));
            }
            assertTrue(readyGate.await(10, TimeUnit.SECONDS));
            startGate.countDown();

            for (Future<Boolean> result : results) {
                assertTrue(result.get(60, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
        }

        assertFalse(processor.processNext(fixture.programId()));
        assertEquals(CONCURRENT_ORDERS, count("reward_inbox_events"));
        assertEquals(CONCURRENT_ORDERS, countWhere("reward_inbox_events", "status = 'COMPLETED'"));
        assertEquals(CONCURRENT_ORDERS, count("reward_contributions"));
        assertEquals(CONCURRENT_ORDERS, countWhere("reward_contributions", "status = 'ALLOCATED'"));
        assertEquals(CONCURRENT_ORDERS, allocationCount());
        assertEquals(CONCURRENT_ORDERS + 1, count("reward_queue_entries"));
        assertEquals(CONCURRENT_ORDERS + 1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT queue_sequence) FROM reward_queue_entries WHERE program_id = ?",
                Long.class,
                fixture.programId()
        ));
        assertEquals(CONCURRENT_ORDERS + 2L, jdbcTemplate.queryForObject(
                "SELECT next_queue_sequence FROM reward_programs WHERE id = ?",
                Long.class,
                fixture.programId()
        ));

        long expectedTotalContribution = CONCURRENT_ORDERS * CONTRIBUTION_AMOUNT_CENTS;
        assertEquals(expectedTotalContribution, sum("reward_contributions", "original_amount_cents"));
        assertEquals(0L, sum("reward_contributions", "remaining_amount_cents"));
        assertEquals(expectedTotalContribution, allocationSum());
        assertEquals(expectedTotalContribution, jdbcTemplate.queryForObject(
                "SELECT allocated_amount_cents FROM reward_queue_entries WHERE id = ?",
                Long.class,
                headEntryId
        ));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(allocated_amount_cents), 0) FROM reward_queue_entries " +
                        "WHERE program_id = ? AND id <> ?",
                Long.class,
                fixture.programId(),
                headEntryId
        ));
    }

    private long count(String table) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE program_id = ?",
                Long.class,
                fixture.programId()
        );
    }

    private long countWhere(String table, String condition) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE program_id = ? AND " + condition,
                Long.class,
                fixture.programId()
        );
    }

    private long sum(String table, String column) {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(" + column + "), 0) FROM " + table + " WHERE program_id = ?",
                Long.class,
                fixture.programId()
        );
    }

    private long allocationSum() {
        return jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(a.amount_cents), 0) FROM reward_allocations a " +
                        "JOIN reward_contributions c ON c.id = a.contribution_id " +
                        "WHERE c.program_id = ?",
                Long.class,
                fixture.programId()
        );
    }

    private long allocationCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_allocations a " +
                        "JOIN reward_contributions c ON c.id = a.contribution_id " +
                        "WHERE c.program_id = ?",
                Long.class,
                fixture.programId()
        );
    }
}
