package com.aicyber.backend.admin.service;

import com.aicyber.backend.admin.dto.AdminInventoryResponse;
import com.aicyber.backend.admin.dto.InventoryItemRequest;
import com.aicyber.backend.admin.dto.InventoryLocationRequest;
import com.aicyber.backend.admin.dto.InventoryStockChangeRequest;
import com.aicyber.backend.admin.repository.AdminInventoryRepository;
import com.aicyber.backend.inventory.model.InventoryMovement;
import com.aicyber.backend.inventory.service.InventoryLedgerService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminInventoryService {
    private static final Set<String> ITEM_STATUSES = Set.of("DRAFT", "ACTIVE", "DISCONTINUED");
    private static final Set<String> LOCATION_TYPES = Set.of("WAREHOUSE", "STORE", "SERVICE");

    private final AdminInventoryRepository repository;
    private final InventoryLedgerService ledgerService;

    public AdminInventoryService(AdminInventoryRepository repository, InventoryLedgerService ledgerService) {
        this.repository = repository;
        this.ledgerService = ledgerService;
    }

    public AdminInventoryResponse snapshot(String query, String category, String status) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        String normalizedCategory = normalizeOptionalCode(category);
        String normalizedStatus = normalizeOptionalCode(status);
        if (!normalizedStatus.isEmpty() && !ITEM_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported inventory status");
        }
        return new AdminInventoryResponse(
                repository.summary(),
                repository.categories(),
                repository.locations(),
                repository.items(normalizedQuery, normalizedCategory, normalizedStatus),
                repository.movements()
        );
    }

    public AdminInventoryResponse.Item createItem(InventoryItemRequest request) {
        InventoryItemRequest normalized = normalizeItem(request);
        return repository.createItem(UUID.randomUUID(), repository.categoryId(normalized.categoryCode()), normalized);
    }

    public AdminInventoryResponse.Item updateItem(UUID itemId, InventoryItemRequest request) {
        if (itemId == null) throw new IllegalArgumentException("Inventory item is required");
        InventoryItemRequest normalized = normalizeItem(request);
        return repository.updateItem(itemId, repository.categoryId(normalized.categoryCode()), normalized);
    }

    public AdminInventoryResponse.Location createLocation(InventoryLocationRequest request) {
        if (request == null) throw new IllegalArgumentException("Location details are required");
        String code = requireText(request.code(), "Location code", 60).toUpperCase(Locale.ROOT);
        String name = requireText(request.name(), "Location name", 160);
        String type = normalizeRequiredCode(request.locationType(), "Location type");
        if (!LOCATION_TYPES.contains(type)) throw new IllegalArgumentException("Unsupported location type");
        return repository.createLocation(UUID.randomUUID(), new InventoryLocationRequest(code, name, type));
    }

    public InventoryMovement receive(InventoryStockChangeRequest request, UUID adminId) {
        requireStockChange(request);
        return ledgerService.receive(
                request.inventoryItemId(), request.locationId(), request.quantity(),
                requireText(request.reason(), "Receipt reason", 300), adminId,
                requireText(request.idempotencyKey(), "Idempotency key", 120)
        );
    }

    public InventoryMovement adjust(InventoryStockChangeRequest request, UUID adminId) {
        requireStockChange(request);
        return ledgerService.adjust(
                request.inventoryItemId(), request.locationId(), request.quantity(),
                requireText(request.reason(), "Adjustment reason", 300), adminId,
                requireText(request.idempotencyKey(), "Idempotency key", 120)
        );
    }

    private InventoryItemRequest normalizeItem(InventoryItemRequest request) {
        if (request == null) throw new IllegalArgumentException("Inventory item details are required");
        String category = normalizeRequiredCode(request.categoryCode(), "Component category");
        String sku = requireText(request.sku(), "SKU", 80).toUpperCase(Locale.ROOT);
        String brand = requireText(request.brand(), "Brand", 100);
        String model = requireText(request.model(), "Model", 160);
        String displayName = requireText(request.displayName(), "Display name", 220);
        String description = optionalText(request.description(), 4000);
        Map<String, Object> specifications = request.specifications() == null ? Map.of() : Map.copyOf(request.specifications());
        if (request.unitCostCents() != null && request.unitCostCents() < 0) throw new IllegalArgumentException("Unit cost cannot be negative");
        if (request.retailPriceCents() == null || request.retailPriceCents() < 0) throw new IllegalArgumentException("Retail price is required");
        String currency = request.currency() == null || request.currency().isBlank()
                ? "AUD" : normalizeRequiredCode(request.currency(), "Currency");
        if (currency.length() != 3) throw new IllegalArgumentException("Currency must use a three-letter code");
        String status = request.status() == null || request.status().isBlank()
                ? "ACTIVE" : normalizeRequiredCode(request.status(), "Status");
        if (!ITEM_STATUSES.contains(status)) throw new IllegalArgumentException("Unsupported inventory status");
        return new InventoryItemRequest(
                category, sku, brand, model, displayName, description, specifications,
                request.unitCostCents(), request.retailPriceCents(), currency, status
        );
    }

    private void requireStockChange(InventoryStockChangeRequest request) {
        if (request == null) throw new IllegalArgumentException("Stock change details are required");
        if (request.inventoryItemId() == null) throw new IllegalArgumentException("Inventory item is required");
        if (request.locationId() == null) throw new IllegalArgumentException("Inventory location is required");
        if (request.quantity() == null) throw new IllegalArgumentException("Quantity is required");
    }

    private String normalizeRequiredCode(String value, String label) {
        return requireText(value, label, 60).toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value) {
        return value == null || value.isBlank() ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String label, int maxLength) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException(label + " is too long");
        return normalized;
    }

    private String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw new IllegalArgumentException("Description is too long");
        return normalized;
    }
}
