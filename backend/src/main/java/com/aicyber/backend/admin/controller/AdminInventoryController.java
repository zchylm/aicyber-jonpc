package com.aicyber.backend.admin.controller;

import com.aicyber.backend.admin.dto.AdminInventoryResponse;
import com.aicyber.backend.admin.dto.InventoryItemRequest;
import com.aicyber.backend.admin.dto.InventoryLocationRequest;
import com.aicyber.backend.admin.dto.InventoryStockChangeRequest;
import com.aicyber.backend.admin.service.AdminInventoryService;
import com.aicyber.backend.inventory.model.InventoryMovement;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/admin/inventory")
public class AdminInventoryController {
    private final AdminInventoryService service;

    public AdminInventoryController(AdminInventoryService service) {
        this.service = service;
    }

    @GetMapping
    public AdminInventoryResponse inventory(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String category,
            @RequestParam(defaultValue = "") String status
    ) {
        return execute(() -> service.snapshot(query, category, status));
    }

    @PostMapping("/items")
    public AdminInventoryResponse.Item createItem(@RequestBody InventoryItemRequest request) {
        return execute(() -> service.createItem(request));
    }

    @PutMapping("/items/{itemId}")
    public AdminInventoryResponse.Item updateItem(
            @PathVariable UUID itemId,
            @RequestBody InventoryItemRequest request
    ) {
        return execute(() -> service.updateItem(itemId, request));
    }

    @PostMapping("/locations")
    public AdminInventoryResponse.Location createLocation(@RequestBody InventoryLocationRequest request) {
        return execute(() -> service.createLocation(request));
    }

    @PostMapping("/receipts")
    public InventoryMovement receive(@RequestBody InventoryStockChangeRequest request, Authentication authentication) {
        return execute(() -> service.receive(request, UUID.fromString(authentication.getName())));
    }

    @PostMapping("/adjustments")
    public InventoryMovement adjust(@RequestBody InventoryStockChangeRequest request, Authentication authentication) {
        return execute(() -> service.adjust(request, UUID.fromString(authentication.getName())));
    }

    private <T> T execute(Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException | DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, readableMessage(exception), exception);
        }
    }

    private String readableMessage(RuntimeException exception) {
        if (exception instanceof IllegalStateException) return exception.getMessage();
        return "SKU, location code or inventory data conflicts with an existing record";
    }
}
