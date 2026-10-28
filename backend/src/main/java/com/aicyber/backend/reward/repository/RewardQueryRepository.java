package com.aicyber.backend.reward.repository;

import com.aicyber.backend.reward.dto.RewardCheckoutPreviewResponse;
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
                "SELECT id, code, name, currency, status, max_positions, max_liability_cents " +
                        "FROM reward_programs WHERE code = ?",
                (resultSet, rowNumber) -> new RewardProgramInfo(
                        resultSet.getObject("id", UUID.class), resultSet.getString("code"),
                        resultSet.getString("name"), resultSet.getString("currency").trim(),
                        resultSet.getString("status"), resultSet.getInt("max_positions"),
                        resultSet.getLong("max_liability_cents")
                ), code
        ).stream().findFirst();
    }

    public RewardPublicSummaryResponse loadSummary(RewardProgramInfo program) {
        long confirmed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_commitments WHERE program_id = ? AND status <> 'VOID' " +
                        "AND (status <> 'PROCESSING' OR reservation_expires_at > CURRENT_TIMESTAMP) " +
                        "AND reward_month = date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date",
                Long.class, program.id());
        long remaining = Math.max(0, program.maxPositions() - confirmed);
        return jdbcTemplate.query(
                "SELECT display_name, rate_basis_points FROM reward_founder_tiers " +
                        "WHERE program_id = ? ORDER BY position_start LIMIT 1",
                (resultSet, rowNumber) -> new RewardPublicSummaryResponse(
                        true, program.name(), remaining == 0 ? "CLOSED" : program.status(), program.currency(),
                        program.maxPositions(), confirmed, remaining, resultSet.getString("display_name"),
                        resultSet.getInt("rate_basis_points"), null, remaining
                ), program.id()
        ).stream().findFirst().orElseGet(() -> new RewardPublicSummaryResponse(
                true, program.name(), "CLOSED", program.currency(), program.maxPositions(), confirmed, remaining,
                null, null, null, 0
        ));
    }

    public RewardCheckoutPreviewResponse checkoutPreview(RewardProgramInfo program, UUID userId, long orderTotalCents, long eligibleSpendCents) {
        long confirmed = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM reward_commitments WHERE program_id = ? AND status <> 'VOID' " +
                        "AND (status <> 'PROCESSING' OR reservation_expires_at > CURRENT_TIMESTAMP) " +
                        "AND reward_month = date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date",
                Long.class, program.id());
        long remaining = Math.max(0, program.maxPositions() - confirmed);
        if (!"ACTIVE".equals(program.status()) || remaining == 0) {
            return RewardCheckoutPreviewResponse.unavailable("The Founder release is fully claimed.", remaining);
        }
        long position = confirmed + 1;
        return jdbcTemplate.query(
                "SELECT display_name, rate_basis_points, cap_cents FROM reward_founder_tiers " +
                        "WHERE program_id = ? AND ? BETWEEN position_start AND position_end",
                (resultSet, rowNumber) -> {
                    int rate = resultSet.getInt("rate_basis_points");
                    long cashback = eligibleSpendCents * rate / 10_000;
                    return new RewardCheckoutPreviewResponse(
                            true, null, resultSet.getString("display_name"), rate, null, cashback,
                            orderTotalCents - cashback, remaining
                    );
                }, program.id(), position
        ).stream().findFirst().orElseGet(() ->
                RewardCheckoutPreviewResponse.unavailable("The Founder release is fully claimed.", remaining));
    }

    public OrderRewardAmounts orderRewardAmounts(UUID orderId) {
        return jdbcTemplate.queryForObject(
                "SELECT amount_cents, product_subtotal_cents - discount_cents - refunded_product_cents AS eligible_spend_cents " +
                        "FROM sales_orders WHERE id = ?",
                (resultSet, rowNumber) -> new OrderRewardAmounts(
                        resultSet.getLong("amount_cents"), resultSet.getLong("eligible_spend_cents")
                ), orderId
        );
    }

    public record OrderRewardAmounts(long totalCents, long eligibleSpendCents) {}

    public List<RewardEntryResponse> findEntries(UUID programId, UUID userId) {
        return jdbcTemplate.query("""
                SELECT c.id, s.order_reference, c.founder_sequence, t.display_name, c.rate_basis_points,
                       c.cap_cents, c.purchase_amount_cents, c.cashback_amount_cents, c.status,
                       c.locked_at, c.paid_at
                FROM reward_commitments c
                JOIN reward_founder_tiers t ON t.id = c.tier_id
                JOIN sales_orders s ON s.id = c.order_id
                WHERE c.program_id = ? AND c.user_id = ? AND c.status <> 'VOID'
                  AND (c.status <> 'PROCESSING' OR c.reservation_expires_at > CURRENT_TIMESTAMP)
                ORDER BY c.locked_at DESC
                """, (resultSet, rowNumber) -> {
            long purchase = resultSet.getLong("purchase_amount_cents");
            long cashback = resultSet.getLong("cashback_amount_cents");
            return new RewardEntryResponse(
                    resultSet.getObject("id", UUID.class), resultSet.getString("order_reference"),
                    resultSet.getLong("founder_sequence"), resultSet.getString("display_name"),
                    resultSet.getInt("rate_basis_points"), resultSet.getLong("cap_cents"), purchase,
                    cashback, purchase - cashback, resultSet.getString("status"),
                    resultSet.getObject("locked_at", java.time.OffsetDateTime.class),
                    resultSet.getObject("paid_at", java.time.OffsetDateTime.class)
            );
        }, programId, userId);
    }
}
