package com.aicyber.backend.catalog;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class SystemCatalogService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public SystemCatalogService(JdbcTemplate jdbc, ObjectMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public List<SystemProduct> allDrafts() {
        return load(false);
    }

    public List<SystemProduct> previews() {
        return load(true);
    }

    private List<SystemProduct> load(boolean previewOnly) {
        String sql = """
                SELECT s.id, s.code, s.name, s.description, s.status, s.preview_enabled,
                       s.listing_order, s.product_range, s.sales_mode, s.source_revision,
                       s.badge, s.planned_preorder_quantity,
                       COALESCE(b.on_hand_quantity, 0) AS on_hand_quantity,
                       COALESCE(b.reorder_point, 1) AS reorder_point,
                       GREATEST(0, COALESCE(b.on_hand_quantity, 0) - (SELECT COUNT(*) FROM system_sale_allocations a WHERE a.system_build_id = s.id)) AS available_quantity,
                       v.display_price_cents, v.recommended_for, v.dispatch_estimate, v.specifications::text
                FROM system_builds s
                JOIN system_build_versions v ON v.system_build_id = s.id
                LEFT JOIN system_inventory_balances b ON b.system_build_id = s.id
                WHERE v.version = (SELECT MAX(version) FROM system_build_versions WHERE system_build_id = s.id)
                  AND (? = FALSE OR s.preview_enabled = TRUE)
                ORDER BY s.listing_order, s.code
                """;
        return jdbc.query(sql, (rs, row) -> new SystemProduct(
                UUID.fromString(rs.getString("id")), rs.getString("code"), rs.getString("name"),
                rs.getString("description"), rs.getLong("display_price_cents"),
                rs.getInt("planned_preorder_quantity"), rs.getInt("on_hand_quantity"), rs.getInt("reorder_point"),
                rs.getInt("available_quantity"), rs.getInt("listing_order"),
                rs.getString("product_range"), rs.getString("sales_mode"), rs.getString("source_revision"),
                rs.getString("badge"), rs.getString("recommended_for"), rs.getString("dispatch_estimate"),
                readSpecs(rs.getString("specifications")), rs.getString("status"), rs.getBoolean("preview_enabled")
        ), previewOnly);
    }

    @Transactional
    public SystemProduct create(SystemProductRequest request) {
        validate(request);
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO system_builds
                    (id, code, name, direction, description, status, preview_enabled, listing_order, badge,
                     planned_preorder_quantity, product_range, sales_mode, source_revision)
                VALUES (?, ?, ?, 'gaming', ?, 'DRAFT', ?, ?, ?, ?, ?, ?, 'ADMIN')
                """, id, request.sku().trim().toUpperCase(), request.name().trim(), blankToNull(request.description()),
                request.previewEnabled(), request.listingOrder(), blankToNull(request.badge()), request.plannedPreorderQuantity(),
                productRange(request), salesMode(request));
        jdbc.update("INSERT INTO system_inventory_balances (system_build_id, on_hand_quantity) VALUES (?, 0)", id);
        jdbc.update("""
                INSERT INTO system_build_versions
                    (id, system_build_id, version, display_price_cents, status, recommended_for, dispatch_estimate, specifications)
                VALUES (?, ?, 1, ?, 'DRAFT', ?, ?, ?::jsonb)
                """, UUID.randomUUID(), id, request.priceCents(), blankToNull(request.recommendedFor()),
                blankToNull(request.dispatchEstimate()), writeSpecs(request.specifications()));
        return find(id);
    }

    @Transactional
    public SystemProduct update(UUID id, SystemProductRequest request) {
        validate(request);
        int changed = jdbc.update("""
                UPDATE system_builds SET code = ?, name = ?, description = ?, preview_enabled = ?,
                    listing_order = ?, badge = ?, planned_preorder_quantity = ?, product_range = ?, sales_mode = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND status = 'DRAFT'
                """, request.sku().trim().toUpperCase(), request.name().trim(), blankToNull(request.description()),
                request.previewEnabled(), request.listingOrder(), blankToNull(request.badge()), request.plannedPreorderQuantity(),
                productRange(request), salesMode(request), id);
        if (changed == 0) throw new IllegalStateException("Only draft systems can be edited");
        Integer allocated = jdbc.queryForObject("SELECT COUNT(*) FROM system_sale_allocations WHERE system_build_id = ?", Integer.class, id);
        if (allocated != null && request.plannedPreorderQuantity() < allocated)
            throw new IllegalArgumentException("Planned quantity cannot be lower than paid system orders");
        Integer nextVersion = jdbc.queryForObject(
                "SELECT COALESCE(MAX(version), 0) + 1 FROM system_build_versions WHERE system_build_id = ?",
                Integer.class, id);
        jdbc.update("""
                INSERT INTO system_build_versions
                    (id, system_build_id, version, display_price_cents, status, recommended_for, dispatch_estimate, specifications)
                VALUES (?, ?, ?, ?, 'DRAFT', ?, ?, ?::jsonb)
                """, UUID.randomUUID(), id, nextVersion, request.priceCents(), blankToNull(request.recommendedFor()),
                blankToNull(request.dispatchEstimate()), writeSpecs(request.specifications()));
        return find(id);
    }

    private SystemProduct find(UUID id) {
        return allDrafts().stream().filter(product -> product.id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalStateException("System draft was not found"));
    }

    private void validate(SystemProductRequest request) {
        if (request == null || request.sku() == null || !request.sku().trim().toUpperCase().matches("[A-Z0-9][A-Z0-9_-]*"))
            throw new IllegalArgumentException("Enter a valid system SKU");
        if (request.name() == null || request.name().isBlank()) throw new IllegalArgumentException("Enter a system name");
        if (request.priceCents() < 0) throw new IllegalArgumentException("Price cannot be negative");
        if (request.plannedPreorderQuantity() < 0) throw new IllegalArgumentException("Planned quantity cannot be negative");
        if (!List.of("STANDARD", "PREORDER", "STOCK_CHECK_REQUIRED").contains(salesMode(request)))
            throw new IllegalArgumentException("Choose a valid sales mode");
        if (request.specifications() == null || request.specifications().isEmpty())
            throw new IllegalArgumentException("Add the system specifications");
        if (request.previewEnabled() && (request.priceCents() == 0 || request.recommendedFor() == null || request.recommendedFor().isBlank()
                || request.dispatchEstimate() == null || request.dispatchEstimate().isBlank()
                || List.of("GPU", "CPU", "Memory", "Storage").stream().anyMatch(key -> request.specifications().get(key) == null
                    || request.specifications().get(key).isBlank())))
            throw new IllegalArgumentException("Add price, dispatch estimate and core specifications before showing the preview");
    }

    private String writeSpecs(Map<String, String> specifications) {
        try {
            return json.writeValueAsString(specifications);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Invalid system specifications", exception);
        }
    }

    private Map<String, String> readSpecs(String value) {
        try {
            return json.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored system specifications are invalid", exception);
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String productRange(SystemProductRequest request) {
        return request.productRange() == null || request.productRange().isBlank() ? "Unassigned" : request.productRange().trim();
    }

    private String salesMode(SystemProductRequest request) {
        return request.salesMode() == null || request.salesMode().isBlank() ? "STANDARD" : request.salesMode().trim().toUpperCase();
    }
}
