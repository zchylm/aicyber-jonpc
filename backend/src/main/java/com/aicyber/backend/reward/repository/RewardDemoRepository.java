package com.aicyber.backend.reward.repository;

import com.aicyber.backend.reward.service.RewardQueryService;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
@Profile("local")
public class RewardDemoRepository {
    private final JdbcTemplate jdbcTemplate;

    public RewardDemoRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public UUID ensureProgram() {
        UUID proposedProgramId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO reward_programs (id, code, name, currency, status) " +
                        "VALUES (?, ?, 'JON. PC Founders Cashback', 'AUD', 'ACTIVE') " +
                        "ON CONFLICT (code) DO NOTHING",
                proposedProgramId,
                RewardQueryService.PROGRAM_CODE
        );
        UUID programId = jdbcTemplate.queryForObject(
                "SELECT id FROM reward_programs WHERE code = ?",
                UUID.class,
                RewardQueryService.PROGRAM_CODE
        );
        jdbcTemplate.update(
                "UPDATE reward_programs SET max_positions = 50, max_liability_cents = 2500000, " +
                        "updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                programId
        );
        insertTier(programId, "LAUNCH", "Launch Founder", 1, 10, 1500, 50_000);
        insertTier(programId, "EARLY", "Early Founder", 11, 25, 1200, 50_000);
        insertTier(programId, "FOUNDER", "Founder", 26, 50, 1000, 50_000);
        return programId;
    }

    private void insertTier(UUID programId, String code, String name, int start, int end, int rate, long cap) {
        jdbcTemplate.update(
                "INSERT INTO reward_founder_tiers " +
                        "(id, program_id, tier_code, display_name, position_start, position_end, rate_basis_points, cap_cents) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (program_id, tier_code) DO UPDATE SET " +
                        "display_name = EXCLUDED.display_name, position_start = EXCLUDED.position_start, " +
                        "position_end = EXCLUDED.position_end, rate_basis_points = EXCLUDED.rate_basis_points, " +
                        "cap_cents = EXCLUDED.cap_cents",
                UUID.randomUUID(), programId, code, name, start, end, rate, cap
        );
    }

    public UUID createSyntheticCustomer() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
                userId,
                "reward-demo-" + userId + "@jonpc.local",
                "demo-account-not-for-login",
                "Queue Demo Customer"
        );
        return userId;
    }

    public UUID latestCommitment(UUID userId, String status) {
        return jdbcTemplate.query(
                "SELECT id FROM reward_commitments WHERE user_id = ? AND status = ? " +
                        "ORDER BY locked_at DESC LIMIT 1",
                (resultSet, rowNumber) -> resultSet.getObject("id", UUID.class), userId, status
        ).stream().findFirst().orElseThrow(() ->
                new IllegalStateException("No " + status.toLowerCase() + " Founder cashback was found for this account"));
    }

    public void markPayable(UUID commitmentId, OffsetDateTime payoutDueAt) {
        int updated = jdbcTemplate.update("""
                UPDATE reward_commitments
                SET status = 'PAYABLE', payable_at = CURRENT_TIMESTAMP, payout_due_at = ?,
                    payout_failure_reason = NULL
                WHERE id = ? AND status = 'LOCKED'
                """, payoutDueAt, commitmentId);
        if (updated != 1) throw new IllegalStateException("Founder cashback could not be made payable");
    }

    public void markPaid(UUID commitmentId, String payoutReference) {
        int updated = jdbcTemplate.update("""
                UPDATE reward_commitments
                SET status = 'PAID', processing_at = COALESCE(processing_at, CURRENT_TIMESTAMP),
                    paid_at = CURRENT_TIMESTAMP, payout_reference = ?, payout_failure_reason = NULL
                WHERE id = ? AND status IN ('PAYABLE', 'PROCESSING', 'FAILED')
                """, payoutReference, commitmentId);
        if (updated != 1) throw new IllegalStateException("Founder cashback is not ready to be paid");
    }

    @Transactional
    public int resetProgramData(UUID programId, UUID currentUserId) {
        requireLocalDatabase();
        List<UUID> orderIds = jdbcTemplate.queryForList(
                "SELECT id FROM sales_orders WHERE user_id = ? " +
                        "UNION SELECT order_id FROM reward_commitments WHERE program_id = ? " +
                        "UNION SELECT order_id FROM reward_queue_entries WHERE program_id = ? " +
                        "UNION SELECT order_id FROM reward_inbox_events WHERE program_id = ? " +
                        "UNION SELECT source_order_id FROM reward_contributions WHERE program_id = ?",
                UUID.class,
                currentUserId,
                programId,
                programId,
                programId,
                programId
        );

        jdbcTemplate.update("DELETE FROM reward_commitments WHERE program_id = ?", programId);

        jdbcTemplate.update(
                "DELETE FROM reward_allocations WHERE contribution_id IN " +
                        "(SELECT id FROM reward_contributions WHERE program_id = ?) " +
                        "OR recipient_queue_entry_id IN " +
                        "(SELECT id FROM reward_queue_entries WHERE program_id = ?)",
                programId,
                programId
        );
        jdbcTemplate.update("DELETE FROM reward_contributions WHERE program_id = ?", programId);
        jdbcTemplate.update("DELETE FROM reward_inbox_events WHERE program_id = ?", programId);
        jdbcTemplate.update("DELETE FROM reward_queue_entries WHERE program_id = ?", programId);
        jdbcTemplate.update(
                "UPDATE reward_programs SET next_founder_sequence = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                programId
        );

        if (!orderIds.isEmpty()) {
            jdbcTemplate.batchUpdate(
                    "DELETE FROM admin_audit_events WHERE entity_type = 'SALES_INVOICE' AND entity_id IN " +
                            "(SELECT id FROM sales_invoices WHERE sales_order_id = ?)",
                    orderIds,
                    orderIds.size(),
                    (statement, orderId) -> statement.setObject(1, orderId)
            );
            jdbcTemplate.batchUpdate(
                    "DELETE FROM invoice_delivery_attempts WHERE invoice_id IN " +
                            "(SELECT id FROM sales_invoices WHERE sales_order_id = ?)",
                    orderIds,
                    orderIds.size(),
                    (statement, orderId) -> statement.setObject(1, orderId)
            );
            jdbcTemplate.batchUpdate(
                    "DELETE FROM sales_invoice_lines WHERE invoice_id IN " +
                            "(SELECT id FROM sales_invoices WHERE sales_order_id = ?)",
                    orderIds,
                    orderIds.size(),
                    (statement, orderId) -> statement.setObject(1, orderId)
            );
            jdbcTemplate.batchUpdate(
                    "DELETE FROM sales_invoices WHERE sales_order_id = ?",
                    orderIds,
                    orderIds.size(),
                    (statement, orderId) -> statement.setObject(1, orderId)
            );
            jdbcTemplate.batchUpdate(
                    "DELETE FROM payments WHERE order_id = ?",
                    orderIds,
                    orderIds.size(),
                    (statement, orderId) -> statement.setObject(1, orderId)
            );
            jdbcTemplate.batchUpdate(
                    "DELETE FROM sales_orders WHERE id = ?",
                    orderIds,
                    orderIds.size(),
                    (statement, orderId) -> statement.setObject(1, orderId)
            );
        }
        jdbcTemplate.update("""
                DELETE FROM custom_build_quotes WHERE build_request_id IN
                    (SELECT id FROM build_requests WHERE user_id = ?)
                """, currentUserId);
        jdbcTemplate.update("DELETE FROM build_requests WHERE user_id = ?", currentUserId);
        jdbcTemplate.update(
                "DELETE FROM users u WHERE u.email LIKE 'reward-demo-%@jonpc.local' " +
                        "AND NOT EXISTS (SELECT 1 FROM sales_orders s WHERE s.user_id = u.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM saved_builds b WHERE b.user_id = u.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM build_requests r WHERE r.user_id = u.id)"
        );
        jdbcTemplate.update("""
                UPDATE system_builds SET planned_preorder_quantity = CASE code
                    WHEN 'JON-STK-5060' THEN 8 WHEN 'JON-STK-5060TI' THEN 8
                    WHEN 'JON-STM-5070' THEN 2 WHEN 'JON-STM-5070TI' THEN 2
                    END, updated_at = CURRENT_TIMESTAMP
                WHERE code IN ('JON-STK-5060', 'JON-STK-5060TI', 'JON-STM-5070', 'JON-STM-5070TI')
                """);
        jdbcTemplate.update("""
                DELETE FROM system_inventory_movements m USING system_builds s
                WHERE s.id = m.system_build_id
                  AND s.code IN ('JON-STK-5060', 'JON-STK-5060TI', 'JON-STM-5070', 'JON-STM-5070TI')
                """);
        jdbcTemplate.update("""
                UPDATE system_inventory_balances b SET on_hand_quantity = CASE s.code
                    WHEN 'JON-STK-5060' THEN 8 WHEN 'JON-STK-5060TI' THEN 8
                    WHEN 'JON-STM-5070' THEN 2 WHEN 'JON-STM-5070TI' THEN 2
                    ELSE b.on_hand_quantity END,
                    version = version + 1, updated_at = CURRENT_TIMESTAMP
                FROM system_builds s WHERE s.id = b.system_build_id
                  AND s.code IN ('JON-STK-5060', 'JON-STK-5060TI', 'JON-STM-5070', 'JON-STM-5070TI')
                """);
        return orderIds.size();
    }

    public void requireLocalDatabase() {
        DataSource dataSource = jdbcTemplate.getDataSource();
        if (dataSource == null) {
            throw new IllegalStateException("Unable to verify the local database connection");
        }
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            String host = URI.create(jdbcUrl.substring("jdbc:".length())).getHost();
            if (!("localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host))) {
                throw new IllegalStateException("Reward demo reset is allowed only on a local database");
            }
        } catch (SQLException | IllegalArgumentException exception) {
            throw new IllegalStateException("Unable to verify the local database connection", exception);
        }
    }
}
