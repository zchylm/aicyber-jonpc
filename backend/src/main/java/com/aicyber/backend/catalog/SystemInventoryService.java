package com.aicyber.backend.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SystemInventoryService {
    private final JdbcTemplate jdbc;

    public SystemInventoryService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional
    public SystemInventoryMovement receive(UUID systemId, SystemStockChangeRequest request, UUID adminId) {
        validate(request, true);
        return apply(systemId, "RECEIPT", request.quantity(), request.reason(), request.idempotencyKey(), adminId);
    }

    @Transactional
    public SystemInventoryMovement adjust(UUID systemId, SystemStockChangeRequest request, UUID adminId) {
        validate(request, false);
        return apply(systemId, "ADJUSTMENT", request.quantity(), request.reason(), request.idempotencyKey(), adminId);
    }

    private SystemInventoryMovement apply(UUID systemId, String type, int delta, String reason, String key, UUID adminId) {
        if (systemId == null) throw new IllegalArgumentException("System is required");
        String normalizedKey = key.trim();
        List<SystemInventoryMovement> existing = findMovement(normalizedKey);
        if (!existing.isEmpty()) return validateReplay(existing.get(0), systemId, type, delta);

        jdbc.update("INSERT INTO system_inventory_balances (system_build_id) VALUES (?) ON CONFLICT DO NOTHING", systemId);
        List<SystemBalance> balances = jdbc.query("""
                SELECT b.on_hand_quantity, b.version,
                       (SELECT COUNT(*) FROM system_sale_allocations a WHERE a.system_build_id = b.system_build_id) AS allocated
                FROM system_inventory_balances b WHERE b.system_build_id = ? FOR UPDATE
                """, (rs, row) -> new SystemBalance(rs.getInt("on_hand_quantity"), rs.getLong("version"), rs.getInt("allocated")), systemId);
        if (balances.isEmpty()) throw new IllegalStateException("System inventory is unavailable");

        existing = findMovement(normalizedKey);
        if (!existing.isEmpty()) return validateReplay(existing.get(0), systemId, type, delta);
        SystemBalance current = balances.get(0);
        int next = Math.addExact(current.onHand(), delta);
        if (next < current.allocated()) throw new IllegalStateException("Stock cannot be lower than paid system orders");
        int changed = jdbc.update("""
                UPDATE system_inventory_balances SET on_hand_quantity = ?, version = version + 1,
                    updated_at = CURRENT_TIMESTAMP WHERE system_build_id = ? AND version = ?
                """, next, systemId, current.version());
        if (changed != 1) throw new IllegalStateException("System inventory changed concurrently");
        jdbc.update("""
                INSERT INTO system_inventory_movements
                    (id, system_build_id, movement_type, quantity_delta, quantity_after, reason, performed_by, idempotency_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, UUID.randomUUID(), systemId, type, delta, next, reason.trim(), adminId, normalizedKey);
        return findMovement(normalizedKey).get(0);
    }

    private List<SystemInventoryMovement> findMovement(String key) {
        return jdbc.query("SELECT * FROM system_inventory_movements WHERE idempotency_key = ?", (rs, row) ->
                new SystemInventoryMovement(rs.getObject("id", UUID.class), rs.getObject("system_build_id", UUID.class),
                        rs.getString("movement_type"), rs.getInt("quantity_delta"), rs.getInt("quantity_after"),
                        rs.getString("reason"), rs.getObject("performed_by", UUID.class), rs.getString("idempotency_key"),
                        rs.getObject("created_at", java.time.OffsetDateTime.class)), key);
    }

    private SystemInventoryMovement validateReplay(SystemInventoryMovement movement, UUID systemId, String type, int delta) {
        if (!movement.systemBuildId().equals(systemId) || !movement.movementType().equals(type) || movement.quantityDelta() != delta)
            throw new IllegalArgumentException("Inventory idempotency key was already used for another operation");
        return movement;
    }

    private void validate(SystemStockChangeRequest request, boolean receipt) {
        if (request == null || request.quantity() == null) throw new IllegalArgumentException("Quantity is required");
        if (receipt && request.quantity() <= 0) throw new IllegalArgumentException("Received quantity must be positive");
        if (!receipt && request.quantity() == 0) throw new IllegalArgumentException("Adjustment cannot be zero");
        if (request.reason() == null || request.reason().isBlank() || request.reason().trim().length() > 300)
            throw new IllegalArgumentException("A reason is required");
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank() || request.idempotencyKey().trim().length() > 120)
            throw new IllegalArgumentException("A valid inventory idempotency key is required");
    }

    private record SystemBalance(int onHand, long version, int allocated) {}
}
