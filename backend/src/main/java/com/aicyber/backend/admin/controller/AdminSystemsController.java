package com.aicyber.backend.admin.controller;

import com.aicyber.backend.catalog.SystemCatalogService;
import com.aicyber.backend.catalog.SystemInventoryMovement;
import com.aicyber.backend.catalog.SystemInventoryService;
import com.aicyber.backend.catalog.SystemProduct;
import com.aicyber.backend.catalog.SystemProductRequest;
import com.aicyber.backend.catalog.SystemStockChangeRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/systems")
public class AdminSystemsController {
    private final SystemCatalogService service;
    private final SystemInventoryService inventory;

    public AdminSystemsController(SystemCatalogService service, SystemInventoryService inventory) {
        this.service = service;
        this.inventory = inventory;
    }

    @PostMapping("/{id}/receipts")
    public SystemInventoryMovement receive(@PathVariable UUID id, @RequestBody SystemStockChangeRequest request,
                                           org.springframework.security.core.Authentication authentication) {
        return execute(() -> inventory.receive(id, request, UUID.fromString(authentication.getName())));
    }

    @PostMapping("/{id}/adjustments")
    public SystemInventoryMovement adjust(@PathVariable UUID id, @RequestBody SystemStockChangeRequest request,
                                          org.springframework.security.core.Authentication authentication) {
        return execute(() -> inventory.adjust(id, request, UUID.fromString(authentication.getName())));
    }

    private <T> T execute(java.util.function.Supplier<T> action) {
        try {
            return action.get();
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        }
    }

    @GetMapping
    public List<SystemProduct> drafts() {
        return service.allDrafts();
    }

    @PostMapping
    public SystemProduct create(@RequestBody SystemProductRequest request) {
        try {
            return service.create(request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "System SKU is already in use", exception);
        }
    }

    @PutMapping("/{id}")
    public SystemProduct update(@PathVariable UUID id, @RequestBody SystemProductRequest request) {
        try {
            return service.update(id, request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, exception.getMessage(), exception);
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "System SKU is already in use", exception);
        }
    }
}
