package com.aicyber.backend.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SystemSaleAllocationService {
    private final JdbcTemplate jdbc;
    private final boolean demoStoreEnabled;

    public SystemSaleAllocationService(JdbcTemplate jdbc,
                                       @Value("${jonpc.catalog.demo-store-enabled:false}") boolean demoStoreEnabled) {
        this.jdbc = jdbc;
        this.demoStoreEnabled = demoStoreEnabled;
    }

    public void reservePendingOrder(UUID orderId) {
        List<CartOrderItem> items = cartItems(orderId);
        if (items.isEmpty()) return;
        jdbc.update("DELETE FROM system_sale_reservations WHERE expires_at <= CURRENT_TIMESTAMP");
        for (CartOrderItem item : items) {
            lockAndValidate(item, orderId);
            jdbc.update("""
                    INSERT INTO system_sale_reservations (sales_order_id, system_build_id, quantity, expires_at)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP + INTERVAL '30 minutes')
                    ON CONFLICT (sales_order_id, system_build_id)
                    DO UPDATE SET quantity = EXCLUDED.quantity, expires_at = EXCLUDED.expires_at
                    """, orderId, item.systemId(), item.quantity());
        }
    }

    public void releasePendingOrder(UUID orderId) {
        jdbc.update("DELETE FROM system_sale_reservations WHERE sales_order_id = ?", orderId);
    }

    public void allocatePaidOrder(UUID orderId) {
        List<CartOrderItem> cartItems = cartItems(orderId);
        if (!cartItems.isEmpty()) {
            reservePendingOrder(orderId);
            for (CartOrderItem item : cartItems) {
                jdbc.update("""
                        INSERT INTO system_sale_allocation_items (sales_order_id, system_build_id, quantity)
                        VALUES (?, ?, ?) ON CONFLICT DO NOTHING
                        """, orderId, item.systemId(), item.quantity());
            }
            releasePendingOrder(orderId);
            return;
        }
        List<OrderSystem> sources = jdbc.query("""
                SELECT b.configuration_snapshot->'answers'->>'systemId' AS system_id,
                       b.configuration_snapshot->'answers'->>'systemSku' AS system_sku
                FROM sales_orders o JOIN build_requests b ON b.id = o.build_request_id
                WHERE o.id = ?
                """, (rs, row) -> new OrderSystem(rs.getString("system_id"), rs.getString("system_sku")), orderId);
        if (sources.isEmpty() || sources.get(0).sku() == null) return;
        OrderSystem source = sources.get(0);
        String lookup = source.id() == null
                ? "SELECT id FROM system_builds WHERE code = ? FOR UPDATE"
                : "SELECT id FROM system_builds WHERE id = ?::uuid FOR UPDATE";
        List<SystemLimit> limits = jdbc.query(lookup,
                (rs, row) -> new SystemLimit(UUID.fromString(rs.getString("id"))),
                source.id() == null ? source.sku() : source.id());
        if (limits.isEmpty()) {
            if (source.sku().startsWith("JON-")) throw new IllegalStateException("System is no longer available");
            return;
        }
        SystemLimit limit = limits.get(0);
        Integer onHand = jdbc.queryForObject(
                "SELECT on_hand_quantity FROM system_inventory_balances WHERE system_build_id = ? FOR UPDATE", Integer.class, limit.id());
        Integer alreadyAllocated = jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_sale_allocations WHERE sales_order_id = ?", Integer.class, orderId);
        if (alreadyAllocated != null && alreadyAllocated > 0) return;
        int used = allocatedQuantity(limit.id());
        if ((onHand == null ? 0 : onHand) - used <= 0) throw new IllegalStateException("System is sold out");
        jdbc.update("INSERT INTO system_sale_allocations (sales_order_id, system_build_id) VALUES (?, ?)", orderId, limit.id());
    }

    private List<CartOrderItem> cartItems(UUID orderId) {
        return jdbc.query("""
                SELECT i.system_build_id, i.name_snapshot, i.unit_price_cents, i.quantity
                FROM sales_orders o
                JOIN build_request_system_items i ON i.build_request_id = o.build_request_id
                WHERE o.id = ? ORDER BY i.created_at, i.id
                """, (rs, row) -> new CartOrderItem(
                rs.getObject("system_build_id", UUID.class), rs.getString("name_snapshot"),
                rs.getLong("unit_price_cents"), rs.getInt("quantity")
        ), orderId);
    }

    private void lockAndValidate(CartOrderItem item, UUID orderId) {
        List<SystemState> states = jdbc.query("""
                SELECT s.sale_enabled, v.display_price_cents,
                       COALESCE(b.on_hand_quantity, 0) AS on_hand_quantity
                FROM system_builds s
                JOIN system_build_versions v ON v.system_build_id = s.id
                JOIN system_inventory_balances b ON b.system_build_id = s.id
                WHERE s.id = ? AND v.version = (SELECT MAX(version) FROM system_build_versions WHERE system_build_id = s.id)
                FOR UPDATE OF s, b
                """, (rs, row) -> new SystemState(
                rs.getBoolean("sale_enabled"), rs.getLong("display_price_cents"), rs.getInt("on_hand_quantity")
        ), item.systemId());
        if (states.isEmpty() || (!demoStoreEnabled && !states.get(0).saleEnabled()))
            throw new IllegalStateException(item.name() + " is no longer available for checkout");
        SystemState state = states.get(0);
        if (state.priceCents() != item.unitPriceCents())
            throw new IllegalStateException(item.name() + " has changed price; review your shopping bag before payment");
        if (demoStoreEnabled) return;
        Integer reserved = jdbc.queryForObject("""
                SELECT COALESCE(SUM(quantity), 0) FROM system_sale_reservations
                WHERE system_build_id = ? AND sales_order_id <> ? AND expires_at > CURRENT_TIMESTAMP
                """, Integer.class, item.systemId(), orderId);
        int available = state.onHand() - allocatedQuantity(item.systemId()) - (reserved == null ? 0 : reserved);
        if (available < item.quantity()) throw new IllegalStateException(item.name() + " does not have enough stock");
    }

    private int allocatedQuantity(UUID systemId) {
        Integer quantity = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM system_sale_allocations WHERE system_build_id = ?)
                     + (SELECT COALESCE(SUM(quantity), 0) FROM system_sale_allocation_items WHERE system_build_id = ?)
                """, Integer.class, systemId, systemId);
        return quantity == null ? 0 : quantity;
    }

    private record OrderSystem(String id, String sku) {}
    private record SystemLimit(UUID id) {}
    private record CartOrderItem(UUID systemId, String name, long unitPriceCents, int quantity) {}
    private record SystemState(boolean saleEnabled, long priceCents, int onHand) {}
}
