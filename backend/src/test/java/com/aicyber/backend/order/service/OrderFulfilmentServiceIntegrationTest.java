package com.aicyber.backend.order.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class OrderFulfilmentServiceIntegrationTest {
    private static final ZoneId MELBOURNE = ZoneId.of("Australia/Melbourne");

    @Autowired OrderFulfilmentService service;
    @Autowired JdbcTemplate jdbc;

    private UUID orderId;
    private UUID userId;

    @AfterEach
    void cleanup() {
        if (orderId != null) {
            jdbc.update("DELETE FROM transactional_email_outbox WHERE aggregate_type = 'SALES_ORDER' AND aggregate_id = ?", orderId);
            jdbc.update("DELETE FROM reward_commitments WHERE order_id = ?", orderId);
            jdbc.update("DELETE FROM sales_orders WHERE id = ?", orderId);
        }
        if (userId != null) jdbc.update("DELETE FROM users WHERE id = ?", userId);
    }

    @Test
    void deliveryIssuePausesAutoConfirmationAndReceiptCompletesFulfilment() {
        createPaidOrder();
        service.begin(orderId);

        var dispatched = service.dispatch(orderId, "Australia Post", "TRACK-100");
        assertEquals("DISPATCHED", dispatched.status());
        assertEquals("TRACK-100", dispatched.trackingNumber());
        assertEquals(1L, emailCount("ORDER_DISPATCHED"));

        OffsetDateTime deliveredAt = OffsetDateTime.now(MELBOURNE).minusMinutes(5);
        var delivered = service.deliver(orderId, deliveredAt);
        assertEquals("DELIVERED", delivered.status());
        assertEquals(deliveredAt.plusHours(48).toInstant(), delivered.autoConfirmAt().toInstant());
        assertEquals(1L, emailCount("DELIVERY_CONFIRMATION_REQUESTED"));

        var issue = service.reportIssue(orderId, userId, "Outer carton arrived damaged.");
        assertEquals("ISSUE_REPORTED", issue.status());
        service.autoConfirmDeliveredOrders();
        assertEquals("ISSUE_REPORTED", service.find(orderId).status());

        var reopened = service.resolveIssue(orderId);
        assertEquals("DELIVERED", reopened.status());
        assertNotNull(reopened.autoConfirmAt());

        jdbc.update("UPDATE order_fulfilments SET auto_confirm_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE sales_order_id = ?", orderId);
        service.autoConfirmDeliveredOrders();
        assertEquals("RECEIVED", service.find(orderId).status());
    }

    @Test
    void onlyTheOwningCustomerCanConfirmReceipt() {
        createPaidOrder();
        service.begin(orderId);
        service.dispatch(orderId, "Australia Post", "TRACK-200");
        service.deliver(orderId, OffsetDateTime.now(MELBOURNE));

        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.confirmReceived(orderId, UUID.randomUUID()));
        assertEquals("RECEIVED", service.confirmReceived(orderId, userId).status());
    }

    @Test
    void deliveryRequiresTrackingAndARecordedDispatch() {
        createPaidOrder();
        service.begin(orderId);

        assertThrows(IllegalArgumentException.class,
                () -> service.dispatch(orderId, "Australia Post", ""));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> service.deliver(orderId, OffsetDateTime.now(MELBOURNE)));

        service.dispatch(orderId, "Australia Post", "TRACK-300");
        assertEquals("DELIVERED", service.deliver(orderId, OffsetDateTime.now(MELBOURNE)).status());
    }

    @Test
    void pickupCanBeRecordedWithoutAParcelDispatch() {
        createPaidOrder();
        jdbc.update("""
                INSERT INTO sales_order_delivery
                    (sales_order_id, delivery_method, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'PICKUP', 'Delivery test', '0400000000', '205 Kensington Rd', 'West Melbourne', 'VIC', '3003')
                """, orderId);
        service.begin(orderId);

        assertThrows(IllegalArgumentException.class,
                () -> service.dispatch(orderId, "Australia Post", "TRACK-PICKUP"));
        assertEquals("DELIVERED", service.deliver(orderId, null).status());
        assertEquals(1L, emailCount("DELIVERY_CONFIRMATION_REQUESTED"));
    }

    private void createPaidOrder() {
        userId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'hash', 'Delivery test')",
                userId, userId + "@example.test");
        jdbc.update("""
                INSERT INTO sales_orders
                    (id, user_id, order_reference, amount_cents, product_subtotal_cents, status, paid_at)
                VALUES (?, ?, ?, 200000, 200000, 'PAID', CURRENT_TIMESTAMP)
                """, orderId, userId, "SO-" + orderId.toString().substring(0, 12).toUpperCase());
    }

    private long emailCount(String messageType) {
        Long count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM transactional_email_outbox
                WHERE aggregate_type = 'SALES_ORDER' AND aggregate_id = ? AND message_type = ?
                """, Long.class, orderId, messageType);
        return count == null ? 0 : count;
    }
}
