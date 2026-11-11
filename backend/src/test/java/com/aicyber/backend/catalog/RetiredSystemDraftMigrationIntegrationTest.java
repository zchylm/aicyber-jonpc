package com.aicyber.backend.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RetiredSystemDraftMigrationIntegrationTest {
    private static final UUID OLD_ID = UUID.fromString("20000000-0000-0000-0000-000000000005");
    @Autowired JdbcTemplate jdbc;

    @Test
    void deletesOnlyUnusedImportedDraftsAndPreservesCurrentProducts() throws Exception {
        var current = currentProducts();
        assertEquals(8, current.size());
        draft();
        executeMigration();
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM system_builds WHERE id = ?", Integer.class, OLD_ID));
        assertEquals(current, currentProducts());
    }

    @ParameterizedTest
    @ValueSource(strings = {"visible", "stock", "referenced", "revised"})
    void refusesToDeleteADraftThatIsInUseOrHasBeenChanged(String condition) throws Exception {
        draft();
        switch (condition) {
            case "visible" -> jdbc.update("UPDATE system_builds SET preview_enabled = TRUE WHERE id = ?", OLD_ID);
            case "stock" -> jdbc.update("INSERT INTO system_inventory_balances (system_build_id, on_hand_quantity) VALUES (?, 1)", OLD_ID);
            case "revised" -> jdbc.update("INSERT INTO system_build_versions (id, system_build_id, version, display_price_cents) VALUES (?, ?, 2, 10000)", UUID.randomUUID(), OLD_ID);
            case "referenced" -> jdbc.update("""
                    INSERT INTO build_requests (id, request_reference, name, email, location, direction,
                        estimated_price, recommended_baseline, configuration_snapshot)
                    VALUES (?, ?, 'Test', 'test@example.test', 'Online', 'gaming', 100, 100,
                        '{"answers":{"systemSku":"S1"}}'::jsonb)
                    """, UUID.randomUUID(), "QA-" + UUID.randomUUID());
        }
        String sql = migrationSql();
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            var savepoint = connection.setSavepoint();
            var error = assertThrows(SQLException.class, () -> {
                try (var statement = connection.createStatement()) { statement.execute(sql); }
            });
            connection.rollback(savepoint);
            assertTrue(error.getMessage().contains("review before removal"));
            return null;
        });
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM system_builds WHERE id = ?", Integer.class, OLD_ID));
    }

    private void draft() {
        jdbc.update("""
                INSERT INTO system_builds (id, code, name, direction, status, preview_enabled, sale_enabled, source_revision)
                VALUES (?, 'S1', 'Legacy QA draft', 'performance', 'DRAFT', FALSE, FALSE, '2026-09-16')
                """, OLD_ID);
        jdbc.update("INSERT INTO system_build_versions (id, system_build_id, version, display_price_cents) VALUES (?, ?, 1, 10000)", UUID.randomUUID(), OLD_ID);
    }

    private java.util.List<java.util.Map<String, Object>> currentProducts() {
        return jdbc.queryForList("""
                SELECT s.id, s.code, s.preview_enabled, s.sale_enabled, v.display_price_cents, v.specifications::text
                FROM system_builds s JOIN system_build_versions v ON v.system_build_id = s.id
                WHERE s.preview_enabled AND v.version = (SELECT MAX(version) FROM system_build_versions WHERE system_build_id = s.id)
                ORDER BY s.code
                """);
    }

    private String migrationSql() throws java.io.IOException {
        return new ClassPathResource("db/migration/V39__remove_unused_legacy_system_drafts.sql")
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private void executeMigration() throws Exception {
        String sql = migrationSql();
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (var statement = connection.createStatement()) { statement.execute(sql); }
            return null;
        });
    }
}
