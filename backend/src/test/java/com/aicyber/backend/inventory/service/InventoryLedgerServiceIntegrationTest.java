package com.aicyber.backend.inventory.service;

import com.aicyber.backend.inventory.model.InventoryMovement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class InventoryLedgerServiceIntegrationTest {
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
                "TEST-CPU-" + shortId()
        );
        jdbcTemplate.update(
                "INSERT INTO inventory_locations (id, code, name) VALUES (?, ?, 'Test Warehouse')",
                locationId,
                "TEST-" + shortId()
        );
    }

    @Test
    void recordsReceiptReservationAndFulfilmentWithoutLosingTheAuditTrail() {
        service.receive(itemId, locationId, 10, "Opening stock", null, key());
        service.reserve(itemId, locationId, 4, null, null, key());
        InventoryMovement fulfilment = service.fulfil(itemId, locationId, 4, null, null, key());

        assertEquals(6, fulfilment.onHandAfter());
        assertEquals(0, fulfilment.reservedAfter());
        assertEquals(3, movementCount());
    }

    @Test
    void replayingTheSameIdempotencyKeyDoesNotChangeStockTwice() {
        String idempotencyKey = key();

        InventoryMovement first = service.receive(itemId, locationId, 10, "Opening stock", null, idempotencyKey);
        InventoryMovement replay = service.receive(itemId, locationId, 10, "Opening stock", null, idempotencyKey);

        assertEquals(first.id(), replay.id());
        assertEquals(10, replay.onHandAfter());
        assertEquals(1, movementCount());
    }

    @Test
    void cannotReuseAnIdempotencyKeyForDifferentStockChanges() {
        String idempotencyKey = key();
        service.receive(itemId, locationId, 10, "Opening stock", null, idempotencyKey);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.receive(itemId, locationId, 5, "Another delivery", null, idempotencyKey)
        );

        assertEquals("Inventory idempotency key was already used for another operation", exception.getMessage());
        assertEquals(1, movementCount());
    }

    @Test
    void cannotReserveMoreThanTheAvailableQuantity() {
        service.receive(itemId, locationId, 3, "Opening stock", null, key());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.reserve(itemId, locationId, 4, null, null, key())
        );

        assertEquals("Insufficient available stock", exception.getMessage());
        assertEquals(1, movementCount());
    }

    @Test
    void negativeAdjustmentCannotTakeStockBelowZero() {
        service.receive(itemId, locationId, 2, "Opening stock", null, key());

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.adjust(itemId, locationId, -3, "Damaged stock", null, key())
        );

        assertEquals("Insufficient stock on hand", exception.getMessage());
        assertEquals(1, movementCount());
    }

    private Integer movementCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements WHERE inventory_item_id = ?",
                Integer.class,
                itemId
        );
    }

    private String key() {
        return "inventory-test-" + UUID.randomUUID();
    }

    private String shortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }
}
