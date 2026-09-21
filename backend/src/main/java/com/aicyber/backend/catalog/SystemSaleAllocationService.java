package com.aicyber.backend.catalog;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SystemSaleAllocationService {
    private final JdbcTemplate jdbc;

    public SystemSaleAllocationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void allocatePaidOrder(UUID orderId) {
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
        Integer used = jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_sale_allocations WHERE system_build_id = ?", Integer.class, limit.id());
        if ((onHand == null ? 0 : onHand) - used <= 0) throw new IllegalStateException("System is sold out");
        jdbc.update("INSERT INTO system_sale_allocations (sales_order_id, system_build_id) VALUES (?, ?)", orderId, limit.id());
    }

    private record OrderSystem(String id, String sku) {}
    private record SystemLimit(UUID id) {}
}
