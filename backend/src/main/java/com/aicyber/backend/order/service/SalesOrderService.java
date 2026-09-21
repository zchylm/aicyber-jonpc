package com.aicyber.backend.order.service;

import com.aicyber.backend.catalog.SystemSaleAllocationService;
import com.aicyber.backend.order.model.BuildRequestOrderSource;
import com.aicyber.backend.order.model.SalesOrder;
import com.aicyber.backend.order.repository.SalesOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
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
        return repository.createFromBuildRequest(source, orderReference());
    }

    @Transactional
    public SalesOrder createFromBuildRequest(UUID userId, String requestReference) {
        if (requestReference == null || requestReference.isBlank()) {
            throw new IllegalArgumentException("Build request reference is required");
        }
        BuildRequestOrderSource source = repository.findBuildRequest(userId, requestReference.trim())
                .orElseThrow(() -> new IllegalArgumentException("Build request was not found for this account"));
        return repository.createFromBuildRequest(source, orderReference());
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

    @Transactional
    public SalesOrder markRewardEligible(UUID orderId) {
        return repository.markRewardEligible(orderId);
    }

    private String orderReference() {
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        return "SO-" + suffix;
    }
}
