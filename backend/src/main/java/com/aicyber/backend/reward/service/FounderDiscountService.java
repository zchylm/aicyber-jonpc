package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.repository.FounderDiscountProgramRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class FounderDiscountService {
    private static final int RATE_BASIS_POINTS = 500;
    private static final int MAX_POSITIONS = 50;
    private final JdbcTemplate jdbc;
    private final FounderDiscountProgramRepository programs;

    public FounderDiscountService(JdbcTemplate jdbc, FounderDiscountProgramRepository programs) {
        this.jdbc = jdbc;
        this.programs = programs;
    }

    @Transactional
    public void reserve(UUID orderId, UUID userId, OffsetDateTime expiresAt) {
        UUID programId = programs.ensureProgram();
        jdbc.queryForObject("SELECT pg_advisory_xact_lock(74102502)", Object.class);
        jdbc.update("UPDATE reward_programs SET name = 'JON.PC Founder 5% Discount', updated_at = CURRENT_TIMESTAMP WHERE id = ?", programId);
        jdbc.update("UPDATE reward_founder_tiers SET display_name = 'Founder 5% Discount', rate_basis_points = 500, cap_cents = 999999999999 WHERE program_id = ?", programId);
        releaseExpired(programId);

        String existingStatus = jdbc.query(
                "SELECT status FROM reward_commitments WHERE program_id = ? AND order_id = ? FOR UPDATE",
                (rs, row) -> rs.getString("status"), programId, orderId
        ).stream().findFirst().orElse(null);
        if (existingStatus != null && !"VOID".equals(existingStatus)) return;

        Integer position = jdbc.queryForObject("""
                SELECT MIN(candidate) FROM generate_series(1, ?) candidate
                WHERE NOT EXISTS (
                    SELECT 1 FROM reward_commitments c
                    WHERE c.program_id = ?
                      AND c.reward_month = date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date
                      AND c.founder_sequence = candidate AND c.status <> 'VOID'
                )
                """, Integer.class, MAX_POSITIONS, programId);
        if (position == null) return;

        OrderAmounts amounts = jdbc.queryForObject(
                "SELECT product_subtotal_cents, shipping_fee_cents FROM sales_orders WHERE id = ? FOR UPDATE",
                (rs, row) -> new OrderAmounts(rs.getLong("product_subtotal_cents"), rs.getLong("shipping_fee_cents")),
                orderId
        );
        if (amounts == null) throw new IllegalArgumentException("Sales order was not found");
        long discountCents = (amounts.productSubtotalCents() * RATE_BASIS_POINTS + 5_000) / 10_000;
        jdbc.update("UPDATE sales_orders SET discount_cents = ?, amount_cents = product_subtotal_cents - ? + shipping_fee_cents, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PENDING_PAYMENT'",
                discountCents, discountCents, orderId);

        UUID tierId = jdbc.queryForObject(
                "SELECT id FROM reward_founder_tiers WHERE program_id = ? AND ? BETWEEN position_start AND position_end",
                UUID.class, programId, position);
        if (existingStatus == null) {
            jdbc.update("""
                    INSERT INTO reward_commitments
                        (id, program_id, tier_id, order_id, user_id, founder_sequence, reward_month, qualifying_at,
                         purchase_amount_cents, eligible_spend_cents, rate_basis_points, cap_cents,
                         cashback_amount_cents, status, reservation_expires_at)
                    VALUES (?, ?, ?, ?, ?, ?, date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date,
                            CURRENT_TIMESTAMP, ?, ?, ?, 999999999999, ?, 'PROCESSING', ?)
                    """, UUID.randomUUID(), programId, tierId, orderId, userId, position,
                    amounts.productSubtotalCents() + amounts.shippingFeeCents(), amounts.productSubtotalCents(),
                    RATE_BASIS_POINTS, discountCents, expiresAt);
        } else {
            jdbc.update("""
                    UPDATE reward_commitments SET tier_id = ?, founder_sequence = ?,
                        reward_month = date_trunc('month', CURRENT_TIMESTAMP AT TIME ZONE 'Australia/Melbourne')::date,
                        qualifying_at = CURRENT_TIMESTAMP, purchase_amount_cents = ?, eligible_spend_cents = ?,
                        rate_basis_points = ?, cap_cents = 999999999999, cashback_amount_cents = ?,
                        status = 'PROCESSING', locked_at = CURRENT_TIMESTAMP, reservation_expires_at = ?,
                        paid_at = NULL, voided_at = NULL
                    WHERE program_id = ? AND order_id = ? AND status = 'VOID'
                    """, tierId, position, amounts.productSubtotalCents() + amounts.shippingFeeCents(),
                    amounts.productSubtotalCents(), RATE_BASIS_POINTS, discountCents, expiresAt, programId, orderId);
        }
    }

    @Transactional
    public void apply(UUID orderId) {
        jdbc.update("UPDATE reward_commitments SET status = 'PAID', paid_at = CURRENT_TIMESTAMP, reservation_expires_at = NULL WHERE order_id = ? AND status = 'PROCESSING'", orderId);
    }

    @Transactional
    public void release(UUID orderId) {
        jdbc.update("UPDATE sales_orders SET amount_cents = product_subtotal_cents + shipping_fee_cents, discount_cents = 0, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = 'PENDING_PAYMENT'", orderId);
        jdbc.update("UPDATE reward_commitments SET status = 'VOID', voided_at = CURRENT_TIMESTAMP, reservation_expires_at = NULL WHERE order_id = ? AND status = 'PROCESSING'", orderId);
    }

    private void releaseExpired(UUID programId) {
        jdbc.update("""
                UPDATE sales_orders s SET amount_cents = s.product_subtotal_cents + s.shipping_fee_cents,
                    discount_cents = 0, updated_at = CURRENT_TIMESTAMP
                FROM reward_commitments c
                WHERE c.order_id = s.id AND c.program_id = ? AND c.status = 'PROCESSING'
                  AND c.reservation_expires_at <= CURRENT_TIMESTAMP AND s.status = 'PENDING_PAYMENT'
                """, programId);
        jdbc.update("UPDATE reward_commitments SET status = 'VOID', voided_at = CURRENT_TIMESTAMP, reservation_expires_at = NULL WHERE program_id = ? AND status = 'PROCESSING' AND reservation_expires_at <= CURRENT_TIMESTAMP", programId);
    }

    private record OrderAmounts(long productSubtotalCents, long shippingFeeCents) {}
}
