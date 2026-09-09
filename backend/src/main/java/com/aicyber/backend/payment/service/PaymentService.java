package com.aicyber.backend.payment.service;

import com.aicyber.backend.order.model.SalesOrder;
import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {
    private final SalesOrderService salesOrderService;
    private final PaymentRepository repository;

    public PaymentService(SalesOrderService salesOrderService, PaymentRepository repository) {
        this.salesOrderService = salesOrderService;
        this.repository = repository;
    }

    @Transactional
    public Payment createMockCheckout(UUID userId, String requestReference, String idempotencyKey) {
        String key = requireIdempotencyKey(idempotencyKey);
        SalesOrder order = salesOrderService.createFromBuildRequest(userId, requestReference);
        if (!"PENDING_PAYMENT".equals(order.status())) {
            return repository.findSuccessfulByOrder(order.id())
                    .orElseThrow(() -> new IllegalStateException("Sales order is not awaiting payment"));
        }
        return repository.create(order, paymentReference(), key);
    }

    private String requireIdempotencyKey(String value) {
        if (value == null || value.isBlank() || value.trim().length() > 120) {
            throw new IllegalArgumentException("A valid payment idempotency key is required");
        }
        return value.trim();
    }

    private String paymentReference() {
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        return "PAY-" + suffix;
    }
}
