package com.aicyber.backend.inventory.repository;

import com.aicyber.backend.inventory.model.InventoryBalance;
import com.aicyber.backend.inventory.model.InventoryMovement;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InventoryRepository {
    private final JdbcTemplate jdbcTemplate;

    public InventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<InventoryMovement> findMovement(String idempotencyKey) {
        return jdbcTemplate.query(
                "SELECT * FROM inventory_movements WHERE idempotency_key = ?",
                (resultSet, rowNum) -> mapMovement(resultSet),
                idempotencyKey
        ).stream().findFirst();
    }

    public void ensureBalance(UUID inventoryItemId, UUID locationId) {
        jdbcTemplate.update(
                "INSERT INTO inventory_balances (inventory_item_id, location_id) VALUES (?, ?) " +
                        "ON CONFLICT (inventory_item_id, location_id) DO NOTHING",
                inventoryItemId,
                locationId
        );
    }

    public InventoryBalance lockBalance(UUID inventoryItemId, UUID locationId) {
        return jdbcTemplate.query(
                "SELECT * FROM inventory_balances WHERE inventory_item_id = ? AND location_id = ? FOR UPDATE",
                (resultSet, rowNum) -> mapBalance(resultSet),
                inventoryItemId,
                locationId
        ).stream().findFirst().orElseThrow(() -> new IllegalStateException("Inventory balance is unavailable"));
    }

    public InventoryBalance updateBalance(InventoryBalance current, int onHandQuantity, int reservedQuantity) {
        int updated = jdbcTemplate.update(
                "UPDATE inventory_balances SET on_hand_quantity = ?, reserved_quantity = ?, " +
                        "version = version + 1, updated_at = CURRENT_TIMESTAMP " +
                        "WHERE inventory_item_id = ? AND location_id = ? AND version = ?",
                onHandQuantity,
                reservedQuantity,
                current.inventoryItemId(),
                current.locationId(),
                current.version()
        );
        if (updated != 1) throw new IllegalStateException("Inventory balance changed concurrently");
        return lockBalance(current.inventoryItemId(), current.locationId());
    }

    public InventoryMovement createMovement(
            UUID inventoryItemId,
            UUID locationId,
            String movementType,
            int onHandDelta,
            int reservedDelta,
            InventoryBalance balance,
            UUID salesOrderId,
            UUID reservationId,
            String reason,
            UUID performedBy,
            String idempotencyKey
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO inventory_movements " +
                        "(id, inventory_item_id, location_id, movement_type, on_hand_delta, reserved_delta, " +
                        "on_hand_after, reserved_after, sales_order_id, reservation_id, reason, performed_by, idempotency_key) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id,
                inventoryItemId,
                locationId,
                movementType,
                onHandDelta,
                reservedDelta,
                balance.onHandQuantity(),
                balance.reservedQuantity(),
                salesOrderId,
                reservationId,
                reason,
                performedBy,
                idempotencyKey
        );
        return findMovement(idempotencyKey).orElseThrow();
    }

    private InventoryBalance mapBalance(ResultSet resultSet) throws SQLException {
        return new InventoryBalance(
                resultSet.getObject("inventory_item_id", UUID.class),
                resultSet.getObject("location_id", UUID.class),
                resultSet.getInt("on_hand_quantity"),
                resultSet.getInt("reserved_quantity"),
                resultSet.getInt("reorder_point"),
                resultSet.getLong("version"),
                resultSet.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    private InventoryMovement mapMovement(ResultSet resultSet) throws SQLException {
        return new InventoryMovement(
                resultSet.getObject("id", UUID.class),
                resultSet.getObject("inventory_item_id", UUID.class),
                resultSet.getObject("location_id", UUID.class),
                resultSet.getString("movement_type"),
                resultSet.getInt("on_hand_delta"),
                resultSet.getInt("reserved_delta"),
                resultSet.getInt("on_hand_after"),
                resultSet.getInt("reserved_after"),
                resultSet.getObject("sales_order_id", UUID.class),
                resultSet.getObject("reservation_id", UUID.class),
                resultSet.getString("reason"),
                resultSet.getObject("performed_by", UUID.class),
                resultSet.getString("idempotency_key"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class)
        );
    }
}
