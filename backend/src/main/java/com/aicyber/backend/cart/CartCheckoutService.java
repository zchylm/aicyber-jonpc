package com.aicyber.backend.cart;

import com.aicyber.backend.auth.model.User;
import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.cart.dto.CartPreparationRequest;
import com.aicyber.backend.cart.dto.CartPreparationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CartCheckoutService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final ObjectMapper json;
    private final boolean demoStoreEnabled;

    public CartCheckoutService(JdbcTemplate jdbc, UserRepository users, ObjectMapper json,
                               @Value("${jonpc.catalog.demo-store-enabled:false}") boolean demoStoreEnabled) {
        this.jdbc = jdbc;
        this.users = users;
        this.json = json;
        this.demoStoreEnabled = demoStoreEnabled;
    }

    @Transactional
    public CartPreparationResponse prepare(UUID userId, CartPreparationRequest request) {
        User user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("User account not found"));
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Your shopping bag is empty");
        }
        if (request.items().size() > 10) throw new IllegalArgumentException("A shopping bag can contain up to 10 systems");

        Set<UUID> unique = new HashSet<>();
        List<CartLine> lines = request.items().stream().map(item -> {
            if (item.systemId() == null || !unique.add(item.systemId()))
                throw new IllegalArgumentException("Each system may appear only once in the shopping bag");
            if (item.quantity() < 1 || item.quantity() > 10)
                throw new IllegalArgumentException("System quantity must be between 1 and 10");
            return loadLine(item.systemId(), item.quantity());
        }).toList();

        long totalCents = lines.stream().mapToLong(CartLine::lineTotalCents).sum();
        String reference = reference();
        UUID requestId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO build_requests
                    (id, user_id, request_reference, name, email, location, contact_requested,
                     direction, estimated_price, recommended_baseline, selected_adjustments, configuration_snapshot)
                VALUES (?, ?, ?, ?, ?, 'Australia', FALSE, 'Store', ?, ?, 0, ?::jsonb)
                """, requestId, userId, reference, user.displayName(), user.email(),
                Math.round(totalCents / 100.0), Math.round(totalCents / 100.0), configurationJson());
        for (CartLine line : lines) {
            jdbc.update("""
                    INSERT INTO build_request_system_items
                        (id, build_request_id, system_build_id, sku_snapshot, name_snapshot,
                         unit_price_cents, quantity, line_total_cents)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """, UUID.randomUUID(), requestId, line.systemId(), line.sku(), line.name(),
                    line.unitPriceCents(), line.quantity(), line.lineTotalCents());
        }
        return new CartPreparationResponse(reference, totalCents);
    }

    private CartLine loadLine(UUID systemId, int quantity) {
        List<CartLine> lines = jdbc.query("""
                SELECT s.id, s.code, s.name, s.sale_enabled,
                       v.display_price_cents,
                       GREATEST(0, COALESCE(b.on_hand_quantity, 0)
                         - (SELECT COUNT(*) FROM system_sale_allocations a WHERE a.system_build_id = s.id)
                         - (SELECT COALESCE(SUM(quantity), 0) FROM system_sale_allocation_items a WHERE a.system_build_id = s.id)
                         - (SELECT COALESCE(SUM(quantity), 0) FROM system_sale_reservations r WHERE r.system_build_id = s.id AND r.expires_at > CURRENT_TIMESTAMP)
                       ) AS available_quantity
                FROM system_builds s
                JOIN system_build_versions v ON v.system_build_id = s.id
                LEFT JOIN system_inventory_balances b ON b.system_build_id = s.id
                WHERE s.id = ? AND s.preview_enabled = TRUE
                  AND v.version = (SELECT MAX(version) FROM system_build_versions WHERE system_build_id = s.id)
                """, (rs, row) -> new CartLine(
                rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name"),
                rs.getLong("display_price_cents"), quantity,
                Math.multiplyExact(rs.getLong("display_price_cents"), quantity),
                rs.getBoolean("sale_enabled"), rs.getInt("available_quantity")
        ), systemId);
        if (lines.isEmpty()) throw new IllegalArgumentException("A system in your shopping bag is no longer available");
        CartLine line = lines.get(0);
        if (!demoStoreEnabled && !line.saleEnabled()) throw new IllegalStateException(line.name() + " is not available for checkout");
        if (!demoStoreEnabled && line.availableQuantity() < quantity) throw new IllegalStateException(line.name() + " does not have enough stock");
        return line;
    }

    private String configurationJson() {
        try {
            return json.writeValueAsString(Map.of(
                    "direction", "store",
                    "answers", Map.of("cartCheckout", "true")
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Shopping bag could not be prepared", exception);
        }
    }

    private String reference() {
        String suffix = UUID.randomUUID().toString().replace("-", "")
                .substring(0, 12).toUpperCase(Locale.ROOT);
        return "CART-" + suffix;
    }

    private record CartLine(UUID systemId, String sku, String name, long unitPriceCents,
                            int quantity, long lineTotalCents, boolean saleEnabled, int availableQuantity) {}
}
