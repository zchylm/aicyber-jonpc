package com.aicyber.backend.inventory.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class InventoryLedgerConcurrencyIntegrationTest {
    private static final int CONCURRENT_RECEIPTS = 100;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private InventoryLedgerService service;

    private UUID itemId;
    private UUID locationId;

    @BeforeEach
    void setUp() {
        itemId = UUID.randomUUID();
        locationId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO inventory_items " +
                        "(id, category_id, sku, brand, model, display_name, retail_price_cents) " +
                        "VALUES (?, '10000000-0000-0000-0000-000000000001', ?, 'AMD', '7600', 'AMD Ryzen 5 7600', 32900)",
                itemId,
                "CONCURRENT-CPU-" + shortId()
        );
        jdbcTemplate.update(
                "INSERT INTO inventory_locations (id, code, name) VALUES (?, ?, 'Concurrency Warehouse')",
                locationId,
                "CONCURRENT-" + shortId()
        );
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM inventory_movements WHERE inventory_item_id = ?", itemId);
        jdbcTemplate.update("DELETE FROM inventory_balances WHERE inventory_item_id = ?", itemId);
        jdbcTemplate.update("DELETE FROM inventory_items WHERE id = ?", itemId);
        jdbcTemplate.update("DELETE FROM inventory_locations WHERE id = ?", locationId);
    }

    @Test
    @Timeout(60)
    void serializesOneHundredChangesWithoutLosingStockOrLedgerEntries() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_RECEIPTS);
        CountDownLatch readyGate = new CountDownLatch(CONCURRENT_RECEIPTS);
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<?>> results = new ArrayList<>();
        try {
            for (int index = 0; index < CONCURRENT_RECEIPTS; index++) {
                int receipt = index;
                results.add(executor.submit(() -> {
                    readyGate.countDown();
                    startGate.await();
                    service.receive(itemId, locationId, 1, "Concurrent receipt", null, "concurrent-receipt-" + receipt);
                    return null;
                }));
            }
            assertTrue(readyGate.await(10, TimeUnit.SECONDS));
            startGate.countDown();
            for (Future<?> result : results) result.get(45, TimeUnit.SECONDS);
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(45, TimeUnit.SECONDS));
        }

        assertEquals(CONCURRENT_RECEIPTS, jdbcTemplate.queryForObject(
                "SELECT on_hand_quantity FROM inventory_balances WHERE inventory_item_id = ? AND location_id = ?",
                Integer.class,
                itemId,
                locationId
        ));
        assertEquals(CONCURRENT_RECEIPTS, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements WHERE inventory_item_id = ? AND location_id = ?",
                Integer.class,
                itemId,
                locationId
        ));
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
