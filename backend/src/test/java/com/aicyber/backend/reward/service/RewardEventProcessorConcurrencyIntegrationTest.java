package com.aicyber.backend.reward.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class RewardEventProcessorConcurrencyIntegrationTest {
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired RewardEventProcessor processor;
    private RewardDatabaseTestFixture fixture;

    @AfterEach void cleanUp() { if (fixture != null) fixture.cleanUp(); }

    @Test @Timeout(90)
    void serializesConcurrentPaymentsAndStopsAtFiftyFounderPositions() throws Exception {
        fixture = new RewardDatabaseTestFixture(jdbcTemplate, 1);
        for (int index = 0; index < 60; index++) fixture.createEvent(fixture.createOrder(250_000));

        ExecutorService executor = Executors.newFixedThreadPool(30);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try {
            for (int index = 0; index < 60; index++) results.add(executor.submit(() -> { start.await(); return processor.processNext(fixture.programId()); }));
            start.countDown();
            for (Future<Boolean> result : results) assertTrue(result.get(60, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS));
        }

        assertEquals(50L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_commitments WHERE program_id = ?", Long.class, fixture.programId()));
        assertEquals(50L, jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT founder_sequence) FROM reward_commitments WHERE program_id = ?", Long.class, fixture.programId()));
        assertEquals(51L, jdbcTemplate.queryForObject(
                "SELECT next_founder_sequence FROM reward_programs WHERE id = ?", Long.class, fixture.programId()));
        assertEquals(60L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_inbox_events WHERE program_id = ? AND status = 'COMPLETED'", Long.class, fixture.programId()));
    }
}
