package com.aicyber.backend.reward.repository;

import com.aicyber.backend.reward.dto.RewardEntryResponse;
import com.aicyber.backend.reward.dto.RewardPublicSummaryResponse;
import com.aicyber.backend.reward.model.RewardProgramInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class RewardQueryRepository {
    private final JdbcTemplate jdbcTemplate;

    public RewardQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<RewardProgramInfo> findProgram(String code) {
        return jdbcTemplate.query(
                "SELECT p.id, p.code, p.name, p.currency, p.status, v.rate_basis_points " +
                        "FROM reward_programs p LEFT JOIN reward_policy_versions v " +
                        "ON v.program_id = p.id AND v.status = 'ACTIVE' " +
                        "WHERE p.code = ?",
                (resultSet, rowNumber) -> new RewardProgramInfo(
                        resultSet.getObject("id", UUID.class),
                        resultSet.getString("code"),
                        resultSet.getString("name"),
                        resultSet.getString("currency").trim(),
                        resultSet.getString("status"),
                        (Integer) resultSet.getObject("rate_basis_points")
                ),
                code
        ).stream().findFirst();
    }

    public RewardPublicSummaryResponse loadSummary(RewardProgramInfo program) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FILTER (WHERE status = 'WAITING') AS waiting_count, " +
                        "COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_count, " +
                        "COALESCE(SUM(allocated_amount_cents), 0) AS total_allocated_cents " +
                        "FROM reward_queue_entries WHERE program_id = ?",
                (resultSet, rowNumber) -> new RewardPublicSummaryResponse(
                        true,
                        program.name(),
                        program.status(),
                        program.currency(),
                        program.rateBasisPoints(),
                        resultSet.getLong("waiting_count"),
                        resultSet.getLong("completed_count"),
                        resultSet.getLong("total_allocated_cents")
                ),
                program.id()
        );
    }

    public List<RewardEntryResponse> findEntries(UUID programId, UUID userId) {
        return jdbcTemplate.query(
                "SELECT q.id, s.order_reference, q.queue_sequence, q.target_amount_cents, " +
                        "q.allocated_amount_cents, q.status, q.joined_at, q.completed_at, " +
                        "CASE WHEN q.status = 'WAITING' THEN " +
                        "(SELECT COUNT(*) + 1 FROM reward_queue_entries ahead " +
                        "WHERE ahead.program_id = q.program_id AND ahead.status = 'WAITING' " +
                        "AND ahead.queue_sequence < q.queue_sequence) END AS current_position, " +
                        "latest.amount_cents AS latest_allocation_amount_cents, latest.created_at AS latest_allocation_at " +
                        "FROM reward_queue_entries q JOIN sales_orders s ON s.id = q.order_id " +
                        "LEFT JOIN LATERAL (SELECT a.amount_cents, a.created_at FROM reward_allocations a " +
                        "WHERE a.recipient_queue_entry_id = q.id ORDER BY a.created_at DESC LIMIT 1) latest ON TRUE " +
                        "WHERE q.program_id = ? AND s.user_id = ? ORDER BY q.joined_at DESC",
                (resultSet, rowNumber) -> {
                    long target = resultSet.getLong("target_amount_cents");
                    long allocated = resultSet.getLong("allocated_amount_cents");
                    return new RewardEntryResponse(
                            resultSet.getObject("id", UUID.class),
                            resultSet.getString("order_reference"),
                            resultSet.getLong("queue_sequence"),
                            (Long) resultSet.getObject("current_position"),
                            target,
                            allocated,
                            target - allocated,
                            percentage(allocated, target),
                            resultSet.getString("status"),
                            resultSet.getObject("joined_at", java.time.OffsetDateTime.class),
                            resultSet.getObject("completed_at", java.time.OffsetDateTime.class),
                            (Long) resultSet.getObject("latest_allocation_amount_cents"),
                            resultSet.getObject("latest_allocation_at", java.time.OffsetDateTime.class)
                    );
                },
                programId,
                userId
        );
    }

    private double percentage(long allocated, long target) {
        return target == 0 ? 0 : Math.round(allocated * 10_000.0 / target) / 100.0;
    }
}
