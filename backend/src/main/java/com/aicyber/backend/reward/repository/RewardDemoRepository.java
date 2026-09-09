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
                        "VALUES (?, ?, 'JON. Queue Rewards', 'AUD', 'ACTIVE') " +
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
                "INSERT INTO reward_policy_versions " +
                        "(id, program_id, version, calculation_type, rate_basis_points, status, effective_from) " +
                        "SELECT ?, ?, 1, 'ORDER_TOTAL_PERCENT', 2500, 'ACTIVE', ? " +
                        "WHERE NOT EXISTS (SELECT 1 FROM reward_policy_versions " +
                        "WHERE program_id = ? AND status = 'ACTIVE')",
                UUID.randomUUID(),
                programId,
                OffsetDateTime.now().minusMinutes(1),
                programId
        );
        return programId;
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

    @Transactional
    public int resetProgramData(UUID programId) {
        requireLocalDatabase();
        List<UUID> orderIds = jdbcTemplate.queryForList(
                "SELECT order_id FROM reward_queue_entries WHERE program_id = ? " +
                        "UNION SELECT order_id FROM reward_inbox_events WHERE program_id = ? " +
                        "UNION SELECT source_order_id FROM reward_contributions WHERE program_id = ?",
                UUID.class,
                programId,
                programId,
                programId
        );

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
                "UPDATE reward_programs SET next_queue_sequence = 1, updated_at = CURRENT_TIMESTAMP WHERE id = ?",
                programId
        );

        if (!orderIds.isEmpty()) {
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
        jdbcTemplate.update(
                "DELETE FROM users u WHERE u.email LIKE 'reward-demo-%@jonpc.local' " +
                        "AND NOT EXISTS (SELECT 1 FROM sales_orders s WHERE s.user_id = u.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM saved_builds b WHERE b.user_id = u.id) " +
                        "AND NOT EXISTS (SELECT 1 FROM build_requests r WHERE r.user_id = u.id)"
        );
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
