package com.aicyber.backend.inventory.service;

import com.aicyber.backend.inventory.model.InventoryBalance;
import com.aicyber.backend.inventory.model.InventoryMovement;
import com.aicyber.backend.inventory.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InventoryLedgerService {
    private final InventoryRepository repository;

    public InventoryLedgerService(InventoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public InventoryMovement receive(
            UUID inventoryItemId,
            UUID locationId,
            int quantity,
            String reason,
            UUID performedBy,
            String idempotencyKey
    ) {
        requirePositive(quantity);
        return apply(inventoryItemId, locationId, "RECEIPT", quantity, 0, null, null, reason, performedBy, idempotencyKey);
    }

    @Transactional
    public InventoryMovement adjust(
            UUID inventoryItemId,
            UUID locationId,
            int quantityDelta,
            String reason,
            UUID performedBy,
            String idempotencyKey
    ) {
        if (quantityDelta == 0) throw new IllegalArgumentException("Inventory adjustment cannot be zero");
        if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Inventory adjustment requires a reason");
        return apply(inventoryItemId, locationId, "ADJUSTMENT", quantityDelta, 0, null, null, reason, performedBy, idempotencyKey);
    }

    @Transactional
    public InventoryMovement reserve(
            UUID inventoryItemId,
            UUID locationId,
            int quantity,
            UUID salesOrderId,
            UUID reservationId,
            String idempotencyKey
    ) {
        requirePositive(quantity);
        return apply(inventoryItemId, locationId, "RESERVATION", 0, quantity, salesOrderId, reservationId, null, null, idempotencyKey);
    }

    @Transactional
    public InventoryMovement release(
            UUID inventoryItemId,
            UUID locationId,
            int quantity,
            UUID salesOrderId,
            UUID reservationId,
            String reason,
            String idempotencyKey
    ) {
        requirePositive(quantity);
        return apply(inventoryItemId, locationId, "RELEASE", 0, -quantity, salesOrderId, reservationId, reason, null, idempotencyKey);
    }

    @Transactional
    public InventoryMovement fulfil(
            UUID inventoryItemId,
            UUID locationId,
            int quantity,
            UUID salesOrderId,
            UUID reservationId,
            String idempotencyKey
    ) {
        requirePositive(quantity);
        return apply(inventoryItemId, locationId, "FULFILMENT", -quantity, -quantity, salesOrderId, reservationId, null, null, idempotencyKey);
    }

    private InventoryMovement apply(
            UUID inventoryItemId,
            UUID locationId,
            String movementType,
            int onHandDelta,
            int reservedDelta,
            UUID salesOrderId,
            UUID reservationId,
            String reason,
            UUID performedBy,
            String idempotencyKey
    ) {
        requireIdentifier(inventoryItemId, "Inventory item");
        requireIdentifier(locationId, "Inventory location");
        String key = requireIdempotencyKey(idempotencyKey);
        InventoryMovement existing = repository.findMovement(key).orElse(null);
        if (existing != null) return validateReplay(existing, inventoryItemId, locationId, movementType, onHandDelta, reservedDelta);

        repository.ensureBalance(inventoryItemId, locationId);
        InventoryBalance current = repository.lockBalance(inventoryItemId, locationId);
        existing = repository.findMovement(key).orElse(null);
        if (existing != null) return validateReplay(existing, inventoryItemId, locationId, movementType, onHandDelta, reservedDelta);

        int nextOnHand = Math.addExact(current.onHandQuantity(), onHandDelta);
        int nextReserved = Math.addExact(current.reservedQuantity(), reservedDelta);
        if (nextOnHand < 0) throw new IllegalStateException("Insufficient stock on hand");
        if (nextReserved < 0) throw new IllegalStateException("Reserved stock cannot be negative");
        if (nextReserved > nextOnHand) throw new IllegalStateException("Insufficient available stock");

        InventoryBalance updated = repository.updateBalance(current, nextOnHand, nextReserved);
        return repository.createMovement(
                inventoryItemId,
                locationId,
                movementType,
                onHandDelta,
                reservedDelta,
                updated,
                salesOrderId,
                reservationId,
                trimToNull(reason),
                performedBy,
                key
        );
    }

    private InventoryMovement validateReplay(
            InventoryMovement existing,
            UUID inventoryItemId,
            UUID locationId,
            String movementType,
            int onHandDelta,
            int reservedDelta
    ) {
        if (!existing.inventoryItemId().equals(inventoryItemId)
                || !existing.locationId().equals(locationId)
                || !existing.movementType().equals(movementType)
                || existing.onHandDelta() != onHandDelta
                || existing.reservedDelta() != reservedDelta) {
            throw new IllegalArgumentException("Inventory idempotency key was already used for another operation");
        }
        return existing;
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Inventory quantity must be positive");
    }

    private void requireIdentifier(UUID value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
    }

    private String requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 120) {
            throw new IllegalArgumentException("A valid inventory idempotency key is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
