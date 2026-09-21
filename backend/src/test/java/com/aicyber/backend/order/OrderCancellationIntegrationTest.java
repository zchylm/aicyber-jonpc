package com.aicyber.backend.order;

import com.aicyber.backend.order.controller.OrderHistoryController;
import com.aicyber.backend.payment.service.PaymentService;
import com.aicyber.backend.order.service.SalesOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class OrderCancellationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired OrderHistoryController history;
    @Autowired PaymentService payments;
    @Autowired SalesOrderService orders;

    @Test
    void unconvertedRequestCanBeCancelledAndHiddenWithoutDeletingIt() {
        UUID customer = user();
        UUID another = user();
        UUID requestId = request(customer);
        assertThrows(ResponseStatusException.class,
                () -> history.cancel(auth(another), requestId));

        history.cancel(auth(customer), requestId);
        assertEquals(0, history.list(auth(customer)).size());
        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM build_requests WHERE id = ?", String.class, requestId));
        history.cancel(auth(customer), requestId);
    }

    @Test
    void pendingMockPaymentCanBeCancelledButPaidOrderCannot() {
        UUID customer = user();
        UUID requestId = request(customer);
        address(requestId);
        String reference = "REQ-" + requestId;
        var payment = payments.createMockCheckout(customer, reference, UUID.randomUUID().toString());

        history.cancel(auth(customer), requestId);
        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM payments WHERE id = ?", String.class, payment.id()));
        assertEquals("CANCELLED", jdbc.queryForObject("SELECT status FROM sales_orders WHERE id = ?", String.class, payment.orderId()));
        assertEquals(0, history.list(auth(customer)).size());

        UUID paidRequestId = request(customer);
        address(paidRequestId);
        var paidAttempt = payments.createMockCheckout(customer, "REQ-" + paidRequestId, UUID.randomUUID().toString());
        orders.markPaid(paidAttempt.orderId());
        assertThrows(ResponseStatusException.class, () -> history.cancel(auth(customer), paidRequestId));
        assertEquals("RECEIVED", jdbc.queryForObject("SELECT status FROM build_requests WHERE id = ?", String.class, paidRequestId));
    }

    private UsernamePasswordAuthenticationToken auth(UUID userId) {
        return new UsernamePasswordAuthenticationToken(userId.toString(), null,
                java.util.List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, display_name, email_verified_at)
                VALUES (?, ?, 'test-hash', 'Cancellation Test User', CURRENT_TIMESTAMP)
                """, id, id + "@example.com");
        return id;
    }

    private UUID request(UUID userId) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO build_requests
                    (id, user_id, request_reference, name, email, location, direction,
                     estimated_price, recommended_baseline, configuration_snapshot)
                VALUES (?, ?, ?, 'Cancellation Test User', ?, 'Melbourne', 'gaming', 2099, 2099,
                    '{"answers":{"systemSku":"TEST-DEMO"}}'::jsonb)
                """, id, userId, "REQ-" + id, userId + "@example.com");
        return id;
    }

    private void address(UUID requestId) {
        jdbc.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Cancellation Test User', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, requestId);
    }
}
