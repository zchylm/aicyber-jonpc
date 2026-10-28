package com.aicyber.backend.order.service;

import com.aicyber.backend.catalog.SystemSaleAllocationService;
import com.aicyber.backend.order.model.BuildRequestOrderSource;
import com.aicyber.backend.order.model.SalesOrder;
import com.aicyber.backend.order.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class SalesOrderService {
    private final SalesOrderRepository repository;
    private final SystemSaleAllocationService allocations;

    public SalesOrderService(SalesOrderRepository repository, SystemSaleAllocationService allocations) {
        this.repository = repository;
        this.allocations = allocations;
    }

    @Transactional
    public SalesOrder createFromLatestBuildRequest(UUID userId) {
        BuildRequestOrderSource source = repository.findLatestUnconvertedBuildRequest(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Submit a build request before creating a demo sales order"
                ));
        SalesOrder order = repository.createFromBuildRequest(source, orderReference());
        allocations.reservePendingOrder(order.id());
        return order;
    }

    @Transactional
    public SalesOrder createFromBuildRequest(UUID userId, String requestReference) {
        return createFromBuildRequest(userId, requestReference, null);
    }

    @Transactional
    public SalesOrder createFromBuildRequest(UUID userId, String requestReference, OffsetDateTime reservationExpiresAt) {
        if (requestReference == null || requestReference.isBlank()) {
            throw new IllegalArgumentException("Build request reference is required");
        }
        BuildRequestOrderSource source = repository.findBuildRequest(userId, requestReference.trim())
                .orElseThrow(() -> new IllegalArgumentException("Build request was not found for this account"));
        SalesOrder order = repository.createFromBuildRequest(source, orderReference());
        if (reservationExpiresAt == null) allocations.reservePendingOrder(order.id());
        else allocations.reservePendingOrderUntil(order.id(), reservationExpiresAt);
        return order;
    }

    @Transactional
    public SalesOrder createStandalone(UUID userId, long amountCents) {
        if (amountCents <= 0) {
            throw new IllegalArgumentException("Sales order amount must be positive");
        }
        return repository.createDirect(userId, amountCents, orderReference());
    }

    @Transactional
    public SalesOrder markPaid(UUID orderId) {
        allocations.allocatePaidOrder(orderId);
        return repository.markPaid(orderId);
    }

    public SalesOrder find(UUID orderId) {
        return repository.findById(orderId).orElseThrow(() -> new IllegalArgumentException("Sales order was not found"));
    }

    @Transactional
    public void releaseReservation(UUID orderId) {
        allocations.releasePendingOrder(orderId);
    }

    @Transactional
    public void cancelPending(UUID orderId) {
        allocations.releasePendingOrder(orderId);
        repository.cancelPending(orderId);
    }

    private String orderReference() {
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        return "SO-" + suffix;
    }
}
