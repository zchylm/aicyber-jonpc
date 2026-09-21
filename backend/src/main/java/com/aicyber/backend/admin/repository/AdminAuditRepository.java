package com.aicyber.backend.admin.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class AdminAuditRepository {
    private final JdbcTemplate jdbcTemplate;

    public AdminAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void recordInvoiceAction(UUID adminUserId, String action, UUID invoiceId, String invoiceNumber) {
        jdbcTemplate.update("""
                INSERT INTO admin_audit_events
                    (id, admin_user_id, action, entity_type, entity_id, details)
                VALUES (?, ?, ?, 'SALES_INVOICE', ?, jsonb_build_object('invoiceNumber', ?))
                """, UUID.randomUUID(), adminUserId, action, invoiceId, invoiceNumber);
    }
}
