package com.aicyber.backend.admin.repository;

import com.aicyber.backend.admin.dto.AdminInventoryResponse;
import com.aicyber.backend.admin.dto.InventoryItemRequest;
import com.aicyber.backend.admin.dto.InventoryLocationRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AdminInventoryRepository {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public AdminInventoryRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public AdminInventoryResponse.Summary summary() {
        return jdbcTemplate.queryForObject("""
                SELECT
                    COUNT(*) AS total_skus,
                    COALESCE(SUM(stock.on_hand_quantity), 0) AS total_on_hand,
                    COALESCE(SUM(stock.reserved_quantity), 0) AS total_reserved,
                    COUNT(*) FILTER (
                        WHERE i.status = 'ACTIVE'
                          AND stock.balance_count > 0
                          AND stock.on_hand_quantity - stock.reserved_quantity <= stock.reorder_point
                    ) AS low_stock_skus,
                    COALESCE(SUM(COALESCE(i.unit_cost_cents, 0) * stock.on_hand_quantity), 0) AS inventory_cost_cents
                FROM inventory_items i
                LEFT JOIN LATERAL (
                    SELECT COALESCE(SUM(on_hand_quantity), 0) AS on_hand_quantity,
                           COALESCE(SUM(reserved_quantity), 0) AS reserved_quantity,
                           COALESCE(SUM(reorder_point), 0) AS reorder_point,
                           COUNT(*) AS balance_count
                    FROM inventory_balances WHERE inventory_item_id = i.id
                ) stock ON TRUE
                """, (resultSet, rowNum) -> new AdminInventoryResponse.Summary(
                resultSet.getLong("total_skus"),
                resultSet.getLong("total_on_hand"),
                resultSet.getLong("total_reserved"),
                resultSet.getLong("low_stock_skus"),
                resultSet.getLong("inventory_cost_cents")
        ));
    }

    public List<AdminInventoryResponse.Category> categories() {
        return jdbcTemplate.query(
                "SELECT id, code, name FROM component_categories WHERE status = 'ACTIVE' ORDER BY sort_order, name",
                (resultSet, rowNum) -> new AdminInventoryResponse.Category(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("code"),
                        resultSet.getString("name")
                )
        );
    }

    public List<AdminInventoryResponse.Location> locations() {
        return jdbcTemplate.query("""
                SELECT l.id, l.code, l.name, l.location_type, l.status,
                       COALESCE(SUM(b.on_hand_quantity), 0) AS on_hand_quantity,
                       COALESCE(SUM(b.reserved_quantity), 0) AS reserved_quantity
                FROM inventory_locations l
                LEFT JOIN inventory_balances b ON b.location_id = l.id
                GROUP BY l.id ORDER BY l.name
                """, (resultSet, rowNum) -> new AdminInventoryResponse.Location(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("code"),
                resultSet.getString("name"),
                resultSet.getString("location_type"),
                resultSet.getString("status"),
                resultSet.getLong("on_hand_quantity"),
                resultSet.getLong("reserved_quantity")
        ));
    }

    public List<AdminInventoryResponse.Item> items(String query, String category, String status) {
        String like = "%" + query.toLowerCase() + "%";
        return jdbcTemplate.query("""
                SELECT i.*, c.code AS category_code, c.name AS category_name,
                       COALESCE(SUM(b.on_hand_quantity), 0) AS on_hand_quantity,
                       COALESCE(SUM(b.reserved_quantity), 0) AS reserved_quantity
                FROM inventory_items i
                JOIN component_categories c ON c.id = i.category_id
                LEFT JOIN inventory_balances b ON b.inventory_item_id = i.id
                WHERE (? = '' OR LOWER(i.sku) LIKE ? OR LOWER(i.display_name) LIKE ?
                       OR LOWER(i.brand) LIKE ? OR LOWER(i.model) LIKE ?)
                  AND (? = '' OR c.code = ?)
                  AND (? = '' OR i.status = ?)
                GROUP BY i.id, c.code, c.name, c.sort_order
                ORDER BY c.sort_order, i.display_name
                """, (resultSet, rowNum) -> mapItem(resultSet),
                query, like, like, like, like, category, category, status, status
        );
    }

    public Optional<AdminInventoryResponse.Item> item(UUID itemId) {
        return jdbcTemplate.query("""
                SELECT i.*, c.code AS category_code, c.name AS category_name,
                       COALESCE(SUM(b.on_hand_quantity), 0) AS on_hand_quantity,
                       COALESCE(SUM(b.reserved_quantity), 0) AS reserved_quantity
                FROM inventory_items i
                JOIN component_categories c ON c.id = i.category_id
                LEFT JOIN inventory_balances b ON b.inventory_item_id = i.id
                WHERE i.id = ? GROUP BY i.id, c.code, c.name
                """, (resultSet, rowNum) -> mapItem(resultSet), itemId).stream().findFirst();
    }

    public List<AdminInventoryResponse.Movement> movements() {
        return jdbcTemplate.query("""
                SELECT m.id, i.sku, i.display_name, l.code AS location_code, m.movement_type,
                       m.on_hand_delta, m.reserved_delta, m.on_hand_after, m.reserved_after,
                       m.reason, u.display_name AS performed_by, m.created_at
                FROM inventory_movements m
                JOIN inventory_items i ON i.id = m.inventory_item_id
                JOIN inventory_locations l ON l.id = m.location_id
                LEFT JOIN users u ON u.id = m.performed_by
                ORDER BY m.created_at DESC, m.id DESC LIMIT 200
                """, (resultSet, rowNum) -> new AdminInventoryResponse.Movement(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("sku"),
                resultSet.getString("display_name"),
                resultSet.getString("location_code"),
                resultSet.getString("movement_type"),
                resultSet.getInt("on_hand_delta"),
                resultSet.getInt("reserved_delta"),
                resultSet.getInt("on_hand_after"),
                resultSet.getInt("reserved_after"),
                resultSet.getString("reason"),
                resultSet.getString("performed_by"),
                resultSet.getObject("created_at", java.time.OffsetDateTime.class)
        ));
    }

    public AdminInventoryResponse.Item createItem(UUID itemId, UUID categoryId, InventoryItemRequest request) {
        jdbcTemplate.update("""
                INSERT INTO inventory_items
                    (id, category_id, sku, brand, model, display_name, description, specifications,
                     unit_cost_cents, retail_price_cents, currency, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)
                """,
                itemId, categoryId, request.sku(), request.brand(), request.model(), request.displayName(),
                request.description(), writeJson(request.specifications()), request.unitCostCents(),
                request.retailPriceCents(), request.currency(), request.status()
        );
        return item(itemId).orElseThrow();
    }

    public AdminInventoryResponse.Item updateItem(UUID itemId, UUID categoryId, InventoryItemRequest request) {
        int updated = jdbcTemplate.update("""
                UPDATE inventory_items SET category_id = ?, sku = ?, brand = ?, model = ?, display_name = ?,
                    description = ?, specifications = ?::jsonb, unit_cost_cents = ?, retail_price_cents = ?,
                    currency = ?, status = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                categoryId, request.sku(), request.brand(), request.model(), request.displayName(),
                request.description(), writeJson(request.specifications()), request.unitCostCents(),
                request.retailPriceCents(), request.currency(), request.status(), itemId
        );
        if (updated != 1) throw new IllegalArgumentException("Inventory item not found");
        return item(itemId).orElseThrow();
    }

    public AdminInventoryResponse.Location createLocation(UUID locationId, InventoryLocationRequest request) {
        jdbcTemplate.update(
                "INSERT INTO inventory_locations (id, code, name, location_type) VALUES (?, ?, ?, ?)",
                locationId, request.code(), request.name(), request.locationType()
        );
        return locations().stream().filter(location -> location.id().equals(locationId)).findFirst().orElseThrow();
    }

    public UUID categoryId(String code) {
        return jdbcTemplate.query(
                "SELECT id FROM component_categories WHERE code = ? AND status = 'ACTIVE'",
                (resultSet, rowNum) -> resultSet.getObject("id", UUID.class),
                code
        ).stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Component category not found"));
    }

    private AdminInventoryResponse.Item mapItem(ResultSet resultSet) throws SQLException {
        long onHand = resultSet.getLong("on_hand_quantity");
        long reserved = resultSet.getLong("reserved_quantity");
        return new AdminInventoryResponse.Item(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("category_code"),
                resultSet.getString("category_name"),
                resultSet.getString("sku"),
                resultSet.getString("brand"),
                resultSet.getString("model"),
                resultSet.getString("display_name"),
                resultSet.getString("description"),
                readJson(resultSet.getString("specifications")),
                resultSet.getObject("unit_cost_cents", Long.class),
                resultSet.getLong("retail_price_cents"),
                resultSet.getString("currency").trim(),
                resultSet.getString("status"),
                onHand,
                reserved,
                onHand - reserved,
                resultSet.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String value) throws SQLException {
        try {
            return objectMapper.readValue(value, Map.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new SQLException("Inventory specifications are invalid", exception);
        }
    }

    private String writeJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new IllegalArgumentException("Inventory specifications are invalid", exception);
        }
    }
}
