package com.aicyber.backend.payment.service;

import com.aicyber.backend.auth.model.User;
import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.order.model.SalesOrder;
import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.repository.PaymentRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class PaymentService {
    private final UserRepository userRepository;
    private final SalesOrderService salesOrderService;
    private final PaymentRepository repository;
    private final JdbcTemplate jdbc;

    public PaymentService(UserRepository userRepository, SalesOrderService salesOrderService, PaymentRepository repository, JdbcTemplate jdbc) {
        this.userRepository = userRepository;
        this.salesOrderService = salesOrderService;
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @Transactional
    public Payment createMockCheckout(UUID userId, String requestReference, String idempotencyKey) {
        requireVerifiedEmail(userId);
        String key = requireIdempotencyKey(idempotencyKey);
        Long owned = jdbc.queryForObject("SELECT COUNT(*) FROM build_requests WHERE user_id = ? AND request_reference = ?", Long.class, userId, requestReference);
        if (owned == null || owned == 0) throw new IllegalArgumentException("Build request not found for this account");
        Long completeAddress = jdbc.queryForObject("""
                SELECT COUNT(*) FROM build_requests b
                JOIN build_delivery_details d ON d.build_request_id = b.id
                WHERE b.user_id = ? AND b.request_reference = ?
                  AND d.recipient_name <> '' AND d.phone <> '' AND d.address_line_1 <> ''
                  AND d.suburb <> '' AND d.state <> '' AND d.postcode <> ''
                """, Long.class, userId, requestReference);
        if (completeAddress == null || completeAddress == 0)
            throw new IllegalStateException("Confirm a complete delivery address before payment");
        SalesOrder order = salesOrderService.createFromBuildRequest(userId, requestReference);
        if (!"PENDING_PAYMENT".equals(order.status())) {
            return repository.findSuccessfulByOrder(order.id())
                    .orElseThrow(() -> new IllegalStateException("Sales order is not awaiting payment"));
        }
        return repository.create(order, paymentReference(), key);
    }

    private void requireVerifiedEmail(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User account not found"));
        if (user.emailVerifiedAt() == null) {
            throw new IllegalStateException("Verify your email before checkout");
        }
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
