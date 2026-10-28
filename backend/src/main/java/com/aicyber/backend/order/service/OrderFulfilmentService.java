package com.aicyber.backend.order.service;

import com.aicyber.backend.email.service.CustomerEmailService;
import com.aicyber.backend.order.dto.OrderFulfilmentResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class OrderFulfilmentService {
    private static final ZoneId MELBOURNE = ZoneId.of("Australia/Melbourne");
    private final JdbcTemplate jdbc;
    private final CustomerEmailService customerEmails;

    public OrderFulfilmentService(JdbcTemplate jdbc, CustomerEmailService customerEmails) {
        this.jdbc = jdbc;
        this.customerEmails = customerEmails;
    }

    public void begin(UUID orderId) {
        jdbc.update("INSERT INTO order_fulfilments (sales_order_id) VALUES (?) ON CONFLICT DO NOTHING", orderId);
    }

    @Transactional
    public OrderFulfilmentResponse dispatch(UUID orderId, String carrier, String trackingNumber) {
        requirePaid(orderId);
        CustomerOrder customer = customer(orderId);
        if (customer.pickup()) {
            throw new IllegalArgumentException("Pickup orders are collected, not dispatched");
        }
        String nextCarrier = requireText(carrier, 120, "Carrier is required");
        String nextTracking = requireText(trackingNumber, 160, "Tracking number is required");
        begin(orderId);
        int updated = jdbc.update("""
                UPDATE order_fulfilments
                SET status = 'DISPATCHED', carrier = ?, tracking_number = ?, dispatched_at = CURRENT_TIMESTAMP,
                    updated_at = CURRENT_TIMESTAMP
                WHERE sales_order_id = ? AND status IN ('PREPARING', 'DISPATCHED')
                """, nextCarrier, nextTracking, orderId);
        if (updated != 1) throw new ResponseStatusException(CONFLICT, "This order cannot be dispatched from its current state");
        customerEmails.orderDispatched(orderId, customer.email(), customer.name(), customer.orderReference(),
                nextCarrier, nextTracking);
        return find(orderId);
    }

    @Transactional
    public OrderFulfilmentResponse deliver(UUID orderId, OffsetDateTime deliveredAt) {
        requirePaid(orderId);
        CustomerOrder customer = customer(orderId);
        if (!customer.pickup() && deliveredAt == null) {
            throw new IllegalArgumentException("Enter the carrier's delivered event time");
        }
        OffsetDateTime delivered = deliveredAt == null ? OffsetDateTime.now(MELBOURNE) : deliveredAt;
        if (delivered.isAfter(OffsetDateTime.now(MELBOURNE).plusMinutes(5))) {
            throw new IllegalArgumentException("Delivered time cannot be in the future");
        }
        begin(orderId);
        String requiredStatus = customer.pickup() ? "PREPARING" : "DISPATCHED";
        int updated = jdbc.update("""
                UPDATE order_fulfilments
                SET status = 'DELIVERED', delivered_at = ?, auto_confirm_at = ?, received_at = NULL,
                    issue_reported_at = NULL, issue_summary = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE sales_order_id = ? AND status = ?
                """, delivered, delivered.plusHours(48), orderId, requiredStatus);
        if (updated != 1) throw new ResponseStatusException(CONFLICT, "This order cannot be marked delivered from its current state");
        OrderFulfilmentResponse response = find(orderId);
        customerEmails.deliveryConfirmationRequested(orderId, customer.email(), customer.name(),
                customer.orderReference(), customer.pickup(), response.autoConfirmAt());
        return response;
    }

    @Transactional
    public OrderFulfilmentResponse confirmReceived(UUID orderId, UUID userId) {
        requireOwned(orderId, userId);
        int updated = jdbc.update("""
                UPDATE order_fulfilments SET status = 'RECEIVED', received_at = CURRENT_TIMESTAMP,
                    auto_confirm_at = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE sales_order_id = ? AND status = 'DELIVERED'
                """, orderId);
        if (updated != 1) throw new ResponseStatusException(CONFLICT, "This delivery is not awaiting confirmation");
        return find(orderId);
    }

    @Transactional
    public OrderFulfilmentResponse reportIssue(UUID orderId, UUID userId, String summary) {
        requireOwned(orderId, userId);
        String issue = requireText(summary, 500, "Briefly describe the delivery issue");
        int updated = jdbc.update("""
                UPDATE order_fulfilments SET status = 'ISSUE_REPORTED', issue_reported_at = CURRENT_TIMESTAMP,
                    issue_summary = ?, auto_confirm_at = NULL, updated_at = CURRENT_TIMESTAMP
                WHERE sales_order_id = ? AND status = 'DELIVERED'
                """, issue, orderId);
        if (updated != 1) throw new ResponseStatusException(CONFLICT, "This delivery is not open for an issue report");
        return find(orderId);
    }

    @Transactional
    public OrderFulfilmentResponse resolveIssue(UUID orderId) {
        int updated = jdbc.update("""
                UPDATE order_fulfilments SET status = 'DELIVERED', issue_reported_at = NULL, issue_summary = NULL,
                    auto_confirm_at = CURRENT_TIMESTAMP + INTERVAL '48 hours', updated_at = CURRENT_TIMESTAMP
                WHERE sales_order_id = ? AND status = 'ISSUE_REPORTED'
                """, orderId);
        if (updated != 1) throw new ResponseStatusException(CONFLICT, "This order has no open delivery issue");
        return find(orderId);
    }

    @Scheduled(fixedDelayString = "${jonpc.orders.delivery-confirmation-check-ms:60000}")
    @Transactional
    public void autoConfirmDeliveredOrders() {
        List<UUID> due = jdbc.queryForList("""
                SELECT sales_order_id FROM order_fulfilments
                WHERE status = 'DELIVERED' AND auto_confirm_at <= CURRENT_TIMESTAMP
                FOR UPDATE SKIP LOCKED
                """, UUID.class);
        for (UUID orderId : due) {
            if (jdbc.update("""
                    UPDATE order_fulfilments SET status = 'RECEIVED', received_at = auto_confirm_at,
                        auto_confirm_at = NULL, updated_at = CURRENT_TIMESTAMP
                    WHERE sales_order_id = ? AND status = 'DELIVERED' AND auto_confirm_at <= CURRENT_TIMESTAMP
                    """, orderId) == 1) {
            }
        }
    }

    public OrderFulfilmentResponse find(UUID orderId) {
        return jdbc.query("""
                SELECT status, carrier, tracking_number, dispatched_at, delivered_at, auto_confirm_at,
                       received_at, issue_reported_at, issue_summary
                FROM order_fulfilments WHERE sales_order_id = ?
                """, (rs, row) -> new OrderFulfilmentResponse(
                rs.getString("status"), rs.getString("carrier"), rs.getString("tracking_number"),
                rs.getObject("dispatched_at", OffsetDateTime.class), rs.getObject("delivered_at", OffsetDateTime.class),
                rs.getObject("auto_confirm_at", OffsetDateTime.class), rs.getObject("received_at", OffsetDateTime.class),
                rs.getObject("issue_reported_at", OffsetDateTime.class), rs.getString("issue_summary")
        ), orderId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order fulfilment was not found"));
    }

    private CustomerOrder customer(UUID orderId) {
        return jdbc.query("""
                SELECT u.email, u.display_name, s.order_reference, d.delivery_method
                FROM sales_orders s
                JOIN users u ON u.id = s.user_id
                LEFT JOIN sales_order_delivery d ON d.sales_order_id = s.id
                WHERE s.id = ?
                """, (rs, row) -> new CustomerOrder(
                rs.getString("email"), rs.getString("display_name"), rs.getString("order_reference"),
                "PICKUP".equals(rs.getString("delivery_method"))
        ), orderId).stream().findFirst().orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Order customer was not found"));
    }

    private void requirePaid(UUID orderId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sales_orders WHERE id = ? AND status = 'PAID'", Long.class, orderId);
        if (count == null || count != 1) throw new ResponseStatusException(NOT_FOUND, "Paid order was not found");
    }

    private void requireOwned(UUID orderId, UUID userId) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM sales_orders WHERE id = ? AND user_id = ? AND status = 'PAID'", Long.class, orderId, userId);
        if (count == null || count != 1) throw new ResponseStatusException(NOT_FOUND, "Paid order was not found for this account");
    }

    private String requireText(String value, int max, String message) {
        if (value == null || value.isBlank() || value.trim().length() > max) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private record CustomerOrder(String email, String name, String orderReference, boolean pickup) {}
}
