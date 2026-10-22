package com.aicyber.backend.payment.service;

import com.aicyber.backend.payment.dto.AdminBankTransferReviewResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminBankTransferService {
    private final JdbcTemplate jdbc;

    public AdminBankTransferService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<AdminBankTransferReviewResponse> reviewQueue() {
        return jdbc.query("""
                SELECT p.id AS payment_id, s.id AS order_id, p.payment_reference, s.order_reference,
                       u.display_name, u.email, p.amount_cents, p.currency,
                       p.customer_reported_at, p.expires_at
                FROM payments p
                JOIN sales_orders s ON s.id = p.order_id
                JOIN users u ON u.id = s.user_id
                WHERE p.provider = 'BANK_TRANSFER' AND p.status = 'PROCESSING'
                ORDER BY p.customer_reported_at ASC, p.created_at ASC
                """, (rs, row) -> new AdminBankTransferReviewResponse(
                rs.getObject("payment_id", java.util.UUID.class), rs.getObject("order_id", java.util.UUID.class),
                rs.getString("payment_reference"), rs.getString("order_reference"), rs.getString("display_name"),
                rs.getString("email"), rs.getLong("amount_cents"), rs.getString("currency"),
                rs.getObject("customer_reported_at", java.time.OffsetDateTime.class),
                rs.getObject("expires_at", java.time.OffsetDateTime.class)
        ));
    }
}
