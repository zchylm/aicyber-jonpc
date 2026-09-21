package com.aicyber.backend.admin.repository;

import com.aicyber.backend.admin.dto.AdminOrderDetailResponse;
import com.aicyber.backend.admin.dto.AdminOrderSummaryResponse;
import com.aicyber.backend.admin.dto.AdminOverviewResponse;
import com.aicyber.backend.admin.dto.AdminRewardSnapshotResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdminQueryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AdminQueryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public AdminOverviewResponse overview() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM sales_orders) AS total_orders,
                    (SELECT COUNT(*) FROM sales_orders WHERE status = 'PENDING_PAYMENT') AS pending_orders,
                    (SELECT COUNT(*) FROM sales_orders WHERE status IN ('PAID', 'REWARD_ELIGIBLE')) AS paid_orders,
                    COALESCE((SELECT SUM(amount_cents) FROM payments WHERE status = 'SUCCEEDED'), 0) AS payment_cents,
                    (SELECT COUNT(*) FROM reward_commitments WHERE status IN ('LOCKED', 'PAYABLE')) AS waiting_rewards,
                    (SELECT COUNT(*) FROM reward_commitments WHERE status = 'PAID') AS completed_rewards,
                    COALESCE((SELECT SUM(cashback_amount_cents) FROM reward_commitments WHERE status <> 'VOID'), 0) AS allocated_cents,
                    (SELECT COUNT(*) FROM reward_inbox_events WHERE status = 'FAILED') AS failed_events,
                    (SELECT COUNT(*) FROM build_requests b
                     WHERE b.user_id IS NOT NULL AND b.status <> 'CANCELLED'
                       AND b.configuration_snapshot->'answers'->>'systemSku' IS NULL
                       AND NOT EXISTS (SELECT 1 FROM custom_build_quotes q WHERE q.build_request_id = b.id)
                       AND NOT EXISTS (SELECT 1 FROM sales_orders s WHERE s.build_request_id = b.id)) AS custom_reviews,
                    (SELECT COUNT(*) FROM system_builds s
                       JOIN system_inventory_balances b ON b.system_build_id = s.id
                       WHERE s.preview_enabled = TRUE AND b.on_hand_quantity <=
                           (SELECT COUNT(*) FROM system_sale_allocations a WHERE a.system_build_id = s.id)) AS sold_out_systems,
                    (SELECT COUNT(*) FROM inventory_items i WHERE i.status = 'ACTIVE'
                       AND EXISTS (SELECT 1 FROM inventory_balances b WHERE b.inventory_item_id = i.id)
                       AND (SELECT COALESCE(SUM(b.on_hand_quantity - b.reserved_quantity), 0)
                            FROM inventory_balances b WHERE b.inventory_item_id = i.id) <=
                           (SELECT COALESCE(SUM(b.reorder_point), 0)
                            FROM inventory_balances b WHERE b.inventory_item_id = i.id)) AS low_stock_components,
                    (SELECT COUNT(*) FROM inventory_items) AS inventory_skus
                    ,(SELECT COUNT(*) FROM sales_orders s WHERE s.status = 'PENDING_PAYMENT'
                        AND EXISTS (SELECT 1 FROM payments p WHERE p.order_id = s.id AND p.status = 'FAILED')
                        AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.order_id = s.id AND p.status = 'SUCCEEDED')) AS payment_issues
                    ,(SELECT COUNT(*) FROM invoice_delivery_attempts d WHERE d.status = 'FAILED'
                        AND d.id = (SELECT d2.id FROM invoice_delivery_attempts d2
                                    WHERE d2.invoice_id = d.invoice_id ORDER BY d2.created_at DESC, d2.id DESC LIMIT 1)) AS email_issues
                    ,COALESCE((SELECT FLOOR(EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - MIN(b.created_at))) / 3600)
                        FROM build_requests b WHERE b.user_id IS NOT NULL AND b.status <> 'CANCELLED'
                          AND b.configuration_snapshot->'answers'->>'systemSku' IS NULL
                          AND NOT EXISTS (SELECT 1 FROM custom_build_quotes q WHERE q.build_request_id = b.id)
                          AND NOT EXISTS (SELECT 1 FROM sales_orders s WHERE s.build_request_id = b.id)), 0) AS oldest_review_hours
                    ,(SELECT COUNT(*) FROM sales_orders s WHERE (s.created_at AT TIME ZONE 'Australia/Melbourne')::date =
                        (CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date) AS orders_today
                    ,COALESCE((SELECT SUM(p.amount_cents) FROM payments p WHERE p.status = 'SUCCEEDED'
                        AND (p.succeeded_at AT TIME ZONE 'Australia/Melbourne')::date =
                            (CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date), 0) AS revenue_today
                    ,COALESCE((SELECT SUM(c.cashback_amount_cents) FROM reward_commitments c
                        WHERE c.status <> 'VOID' AND (c.locked_at AT TIME ZONE 'Australia/Melbourne')::date =
                            (CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date), 0) AS rewards_allocated_today
                    ,(SELECT COUNT(*) FROM build_requests b WHERE b.user_id IS NOT NULL
                        AND b.configuration_snapshot->'answers'->>'systemSku' IS NULL
                        AND (b.created_at AT TIME ZONE 'Australia/Melbourne')::date =
                            (CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date) AS custom_today
                """, (resultSet, rowNum) -> new AdminOverviewResponse(
                resultSet.getLong("total_orders"),
                resultSet.getLong("pending_orders"),
                resultSet.getLong("paid_orders"),
                resultSet.getLong("payment_cents"),
                resultSet.getLong("waiting_rewards"),
                resultSet.getLong("completed_rewards"),
                resultSet.getLong("allocated_cents"),
                resultSet.getLong("failed_events"),
                resultSet.getLong("custom_reviews"),
                resultSet.getLong("sold_out_systems"),
                resultSet.getLong("low_stock_components"),
                resultSet.getLong("inventory_skus"),
                resultSet.getLong("payment_issues"),
                resultSet.getLong("email_issues"),
                resultSet.getLong("oldest_review_hours"),
                resultSet.getLong("orders_today"),
                resultSet.getLong("revenue_today"),
                resultSet.getLong("rewards_allocated_today"),
                resultSet.getLong("custom_today"),
                inventoryAlerts(),
                recentActivity()
        ));
    }

    private List<AdminOverviewResponse.InventoryAlert> inventoryAlerts() {
        return jdbcTemplate.query("""
                SELECT item_type, sku, display_name, available_quantity, reorder_point FROM (
                    SELECT 'COMPONENT' AS item_type, i.sku, i.display_name,
                           COALESCE(SUM(b.on_hand_quantity - b.reserved_quantity), 0) AS available_quantity,
                           COALESCE(SUM(b.reorder_point), 0) AS reorder_point
                    FROM inventory_items i JOIN inventory_balances b ON b.inventory_item_id = i.id
                    WHERE i.status = 'ACTIVE'
                    GROUP BY i.id
                    HAVING SUM(b.on_hand_quantity - b.reserved_quantity) <= SUM(b.reorder_point)
                    UNION ALL
                    SELECT 'SYSTEM', s.code, s.name,
                           GREATEST(0, b.on_hand_quantity -
                               (SELECT COUNT(*) FROM system_sale_allocations a WHERE a.system_build_id = s.id)),
                           b.reorder_point
                    FROM system_builds s JOIN system_inventory_balances b ON b.system_build_id = s.id
                    WHERE s.preview_enabled = TRUE AND GREATEST(0, b.on_hand_quantity -
                        (SELECT COUNT(*) FROM system_sale_allocations a WHERE a.system_build_id = s.id)) <= b.reorder_point
                ) alerts
                ORDER BY available_quantity, display_name LIMIT 6
                """, (rs, row) -> new AdminOverviewResponse.InventoryAlert(
                rs.getString("item_type"), rs.getString("sku"), rs.getString("display_name"),
                rs.getLong("available_quantity"), rs.getLong("reorder_point")
        ));
    }

    private List<AdminOverviewResponse.ActivityItem> recentActivity() {
        return jdbcTemplate.query("""
                SELECT type, title, detail, amount_cents, occurred_at FROM (
                    SELECT 'PAYMENT' AS type, 'Order ' || s.order_reference || ' paid' AS title,
                           COALESCE(b.configuration_snapshot->'answers'->>'systemName', 'Payment confirmed') AS detail,
                           p.amount_cents, p.succeeded_at AS occurred_at
                    FROM payments p JOIN sales_orders s ON s.id = p.order_id
                    LEFT JOIN build_requests b ON b.id = s.build_request_id WHERE p.status = 'SUCCEEDED'
                    UNION ALL
                    SELECT 'CUSTOM_BUILD', 'Custom build submitted',
                           INITCAP(b.direction) || ' · ' || b.request_reference, NULL::bigint, b.created_at
                    FROM build_requests b WHERE b.user_id IS NOT NULL
                      AND b.configuration_snapshot->'answers'->>'systemSku' IS NULL
                    UNION ALL
                    SELECT 'INVENTORY', i.display_name || ' inventory updated',
                           (m.on_hand_after - m.on_hand_delta)::text || ' → ' || m.on_hand_after::text,
                           NULL::bigint, m.created_at
                    FROM inventory_movements m JOIN inventory_items i ON i.id = m.inventory_item_id
                    UNION ALL
                    SELECT 'INVENTORY', s.name || ' inventory updated',
                           (m.quantity_after - m.quantity_delta)::text || ' → ' || m.quantity_after::text,
                           NULL::bigint, m.created_at
                    FROM system_inventory_movements m JOIN system_builds s ON s.id = m.system_build_id
                    UNION ALL
                    SELECT 'REWARD', 'Founder cashback locked',
                           s.order_reference, c.cashback_amount_cents, c.locked_at
                    FROM reward_commitments c
                    JOIN sales_orders s ON s.id = c.order_id
                    WHERE c.status <> 'VOID'
                ) activity ORDER BY occurred_at DESC LIMIT 8
                """, (rs, row) -> new AdminOverviewResponse.ActivityItem(
                rs.getString("type"), rs.getString("title"), rs.getString("detail"),
                rs.getObject("amount_cents") == null ? null : rs.getLong("amount_cents"),
                rs.getObject("occurred_at", java.time.OffsetDateTime.class)
        ));
    }

    public long countOrders(String query, String status) {
        return jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sales_orders s
                JOIN users u ON u.id = s.user_id
                LEFT JOIN sales_invoices i ON i.sales_order_id = s.id
                WHERE (? = '' OR LOWER(s.order_reference) LIKE ? OR LOWER(u.email) LIKE ?
                       OR LOWER(u.display_name) LIKE ? OR LOWER(u.customer_reference) LIKE ?
                       OR LOWER(COALESCE(i.invoice_number, '')) LIKE ?
                       OR EXISTS (SELECT 1 FROM payments px WHERE px.order_id = s.id AND LOWER(px.payment_reference) LIKE ?))
                  AND (? = '' OR s.status = ?)
                """, Long.class, query, like(query), like(query), like(query), like(query), like(query), like(query), status, status);
    }

    public List<AdminOrderSummaryResponse> findOrders(String query, String status, int limit, int offset) {
        return jdbcTemplate.query("""
                SELECT s.id, s.order_reference, u.display_name, u.email, u.customer_reference, s.amount_cents, s.currency,
                       s.status, p.status AS payment_status, p.payment_reference, q.status AS reward_status,
                       i.invoice_number, s.created_at
                FROM sales_orders s
                JOIN users u ON u.id = s.user_id
                LEFT JOIN LATERAL (
                    SELECT status, payment_reference FROM payments
                    WHERE order_id = s.id ORDER BY created_at DESC LIMIT 1
                ) p ON TRUE
                LEFT JOIN LATERAL (
                    SELECT status FROM reward_commitments
                    WHERE order_id = s.id ORDER BY locked_at DESC LIMIT 1
                ) q ON TRUE
                LEFT JOIN sales_invoices i ON i.sales_order_id = s.id
                WHERE (? = '' OR LOWER(s.order_reference) LIKE ? OR LOWER(u.email) LIKE ?
                       OR LOWER(u.display_name) LIKE ? OR LOWER(u.customer_reference) LIKE ?
                       OR LOWER(COALESCE(i.invoice_number, '')) LIKE ?
                       OR LOWER(COALESCE(p.payment_reference, '')) LIKE ?)
                  AND (? = '' OR s.status = ?)
                ORDER BY s.created_at DESC
                LIMIT ? OFFSET ?
                """, (resultSet, rowNum) -> new AdminOrderSummaryResponse(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("order_reference"),
                resultSet.getString("display_name"),
                resultSet.getString("email"),
                resultSet.getString("customer_reference"),
                resultSet.getLong("amount_cents"),
                resultSet.getString("currency"),
                resultSet.getString("status"),
                resultSet.getString("payment_status"),
                resultSet.getString("payment_reference"),
                resultSet.getString("invoice_number"),
                resultSet.getString("reward_status"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class)
        ), query, like(query), like(query), like(query), like(query), like(query), like(query), status, status, limit, offset);
    }

    public Optional<AdminOrderDetailResponse> findOrder(UUID orderId) {
        Optional<OrderBase> order = jdbcTemplate.query("""
                SELECT s.id, s.order_reference, u.display_name, u.email, u.customer_reference, s.amount_cents, s.currency,
                       s.status, s.paid_at, s.reward_eligible_at, s.created_at, s.updated_at,
                       b.request_reference, b.direction, b.configuration_snapshot
                FROM sales_orders s
                JOIN users u ON u.id = s.user_id
                LEFT JOIN build_requests b ON b.id = s.build_request_id
                WHERE s.id = ?
                """, (resultSet, rowNum) -> new OrderBase(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("order_reference"),
                resultSet.getString("display_name"),
                resultSet.getString("email"),
                resultSet.getString("customer_reference"),
                resultSet.getLong("amount_cents"),
                resultSet.getString("currency"),
                resultSet.getString("status"),
                resultSet.getObject("paid_at", java.time.OffsetDateTime.class),
                resultSet.getObject("reward_eligible_at", java.time.OffsetDateTime.class),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                resultSet.getObject("updated_at", java.time.OffsetDateTime.class),
                resultSet.getString("request_reference"),
                resultSet.getString("direction"),
                json(resultSet.getString("configuration_snapshot"))
        ), orderId).stream().findFirst();
        return order.map(value -> new AdminOrderDetailResponse(
                value.id(), value.orderReference(), value.customerName(), value.customerEmail(), value.customerReference(),
                value.amountCents(), value.currency(), value.status(), value.paidAt(), value.rewardEligibleAt(),
                value.createdAt(), value.updatedAt(), value.buildRequestReference(), value.direction(),
                value.configuration(), delivery(orderId).orElse(null), payments(orderId), invoice(orderId).orElse(null), reward(orderId).orElse(null)
        ));
    }

    public AdminRewardSnapshotResponse rewards() {
        return new AdminRewardSnapshotResponse(campaignSummary(), commitments(), failedEvents());
    }

    private List<AdminOrderDetailResponse.PaymentItem> payments(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT id, payment_reference, provider, provider_payment_id, amount_cents, currency,
                       status, failure_reason, created_at, succeeded_at
                FROM payments WHERE order_id = ? ORDER BY created_at DESC
                """, (resultSet, rowNum) -> new AdminOrderDetailResponse.PaymentItem(
                resultSet.getObject("id", UUID.class), resultSet.getString("payment_reference"),
                resultSet.getString("provider"), resultSet.getString("provider_payment_id"),
                resultSet.getLong("amount_cents"), resultSet.getString("currency"), resultSet.getString("status"),
                resultSet.getString("failure_reason"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class),
                resultSet.getObject("succeeded_at", java.time.OffsetDateTime.class)
        ), orderId);
    }

    private Optional<AdminOrderDetailResponse.DeliveryItem> delivery(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT recipient_name, phone, address_line_1, address_line_2, suburb, state, postcode, country
                FROM sales_order_delivery WHERE sales_order_id = ?
                """, (rs, row) -> new AdminOrderDetailResponse.DeliveryItem(
                rs.getString("recipient_name"), rs.getString("phone"), rs.getString("address_line_1"),
                rs.getString("address_line_2"), rs.getString("suburb"), rs.getString("state"),
                rs.getString("postcode"), rs.getString("country")
        ), orderId).stream().findFirst();
    }

    private Optional<AdminOrderDetailResponse.RewardItem> reward(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT c.id, c.founder_sequence, t.display_name, c.rate_basis_points, c.cap_cents,
                       c.purchase_amount_cents, c.cashback_amount_cents, c.status, c.locked_at, c.payable_at, c.paid_at
                FROM reward_commitments c JOIN reward_founder_tiers t ON t.id = c.tier_id
                WHERE c.order_id = ?
                """, (resultSet, rowNum) -> new AdminOrderDetailResponse.RewardItem(
                resultSet.getObject("id", UUID.class), resultSet.getLong("founder_sequence"),
                resultSet.getString("display_name"), resultSet.getInt("rate_basis_points"),
                resultSet.getLong("cap_cents"), resultSet.getLong("purchase_amount_cents"),
                resultSet.getLong("cashback_amount_cents"), resultSet.getString("status"),
                resultSet.getObject("locked_at", java.time.OffsetDateTime.class),
                resultSet.getObject("payable_at", java.time.OffsetDateTime.class),
                resultSet.getObject("paid_at", java.time.OffsetDateTime.class)
        ), orderId).stream().findFirst();
    }

    private Optional<AdminOrderDetailResponse.InvoiceItem> invoice(UUID orderId) {
        return jdbcTemplate.query("""
                SELECT i.id, i.invoice_number, i.status, i.subtotal_ex_gst_cents, i.gst_cents,
                       i.total_cents, i.issued_at, d.status AS delivery_status, d.sent_at
                FROM sales_invoices i
                LEFT JOIN LATERAL (
                    SELECT status, sent_at FROM invoice_delivery_attempts
                    WHERE invoice_id = i.id ORDER BY created_at DESC, id DESC LIMIT 1
                ) d ON TRUE
                WHERE i.sales_order_id = ?
                """, (resultSet, rowNum) -> new AdminOrderDetailResponse.InvoiceItem(
                resultSet.getObject("id", UUID.class), resultSet.getString("invoice_number"),
                resultSet.getString("status"), resultSet.getLong("subtotal_ex_gst_cents"),
                resultSet.getLong("gst_cents"), resultSet.getLong("total_cents"),
                resultSet.getObject("issued_at", java.time.OffsetDateTime.class),
                resultSet.getString("delivery_status"),
                resultSet.getObject("sent_at", java.time.OffsetDateTime.class)
        ), orderId).stream().findFirst();
    }

    private AdminRewardSnapshotResponse.CampaignSummary campaignSummary() {
        return jdbcTemplate.queryForObject("""
                SELECT COALESCE(p.max_positions, 50) AS max_positions,
                       COALESCE(p.max_liability_cents, 2500000) AS max_liability_cents,
                       COALESCE(t.confirmed, 0) AS confirmed,
                       COALESCE(t.committed, 0) AS committed,
                       COALESCE(t.paid, 0) AS paid
                FROM (SELECT 1) seed
                LEFT JOIN reward_programs p ON p.code = 'JON_FOUNDERS_CASHBACK'
                LEFT JOIN LATERAL (
                    SELECT COUNT(*) FILTER (WHERE c.status <> 'VOID') AS confirmed,
                           COALESCE(SUM(c.cashback_amount_cents) FILTER (WHERE c.status <> 'VOID'), 0) AS committed,
                           COALESCE(SUM(c.cashback_amount_cents) FILTER (WHERE c.status = 'PAID'), 0) AS paid
                    FROM reward_commitments c WHERE c.program_id = p.id
                ) t ON TRUE
                """, (rs, row) -> {
            long confirmed = rs.getLong("confirmed");
            long committed = rs.getLong("committed");
            long liability = rs.getLong("max_liability_cents");
            return new AdminRewardSnapshotResponse.CampaignSummary(
                    rs.getInt("max_positions"), confirmed,
                    Math.max(0, rs.getInt("max_positions") - confirmed), liability, committed,
                    rs.getLong("paid"), Math.max(0, liability - committed)
            );
        });
    }

    private List<AdminRewardSnapshotResponse.CommitmentItem> commitments() {
        return jdbcTemplate.query("""
                SELECT c.id, c.founder_sequence, t.display_name, s.order_reference, u.email,
                       c.purchase_amount_cents, c.rate_basis_points, c.cap_cents, c.cashback_amount_cents,
                       c.status, c.locked_at
                FROM reward_commitments c
                JOIN reward_founder_tiers t ON t.id = c.tier_id
                JOIN sales_orders s ON s.id = c.order_id
                JOIN users u ON u.id = c.user_id
                ORDER BY c.founder_sequence DESC LIMIT 100
                """, (resultSet, rowNum) -> new AdminRewardSnapshotResponse.CommitmentItem(
                resultSet.getObject("id", UUID.class), resultSet.getLong("founder_sequence"),
                resultSet.getString("display_name"), resultSet.getString("order_reference"),
                resultSet.getString("email"), resultSet.getLong("purchase_amount_cents"),
                resultSet.getInt("rate_basis_points"), resultSet.getLong("cap_cents"),
                resultSet.getLong("cashback_amount_cents"), resultSet.getString("status"),
                resultSet.getObject("locked_at", java.time.OffsetDateTime.class)
        ));
    }

    private List<AdminRewardSnapshotResponse.FailedEventItem> failedEvents() {
        return jdbcTemplate.query("""
                SELECT e.id, e.external_event_id, s.order_reference, e.attempt_count, e.last_error, e.created_at
                FROM reward_inbox_events e JOIN sales_orders s ON s.id = e.order_id
                WHERE e.status = 'FAILED' ORDER BY e.created_at DESC LIMIT 100
                """, (resultSet, rowNum) -> new AdminRewardSnapshotResponse.FailedEventItem(
                resultSet.getObject("id", UUID.class), resultSet.getString("external_event_id"),
                resultSet.getString("order_reference"), resultSet.getInt("attempt_count"),
                resultSet.getString("last_error"), resultSet.getObject("created_at", java.time.OffsetDateTime.class)
        ));
    }

    private String like(String query) {
        return "%" + query + "%";
    }

    private java.util.Map<String, Object> json(String value) {
        if (value == null) return java.util.Map.of();
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored order configuration is invalid", exception);
        }
    }

    private record OrderBase(
            UUID id,
            String orderReference,
            String customerName,
            String customerEmail,
            String customerReference,
            long amountCents,
            String currency,
            String status,
            java.time.OffsetDateTime paidAt,
            java.time.OffsetDateTime rewardEligibleAt,
            java.time.OffsetDateTime createdAt,
            java.time.OffsetDateTime updatedAt,
            String buildRequestReference,
            String direction,
            java.util.Map<String, Object> configuration
    ) {
    }
}
