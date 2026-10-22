package com.aicyber.backend.payment.service;

import com.aicyber.backend.payment.dto.AdminBankTransferReviewResponse;
import com.aicyber.backend.payment.dto.BankTransferResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "jonpc.payments.bank-transfer.bank-name=Test Bank",
        "jonpc.payments.bank-transfer.account-name=Test Company",
        "jonpc.payments.bank-transfer.bsb=000-000",
        "jonpc.payments.bank-transfer.account-number=00000000",
        "jonpc.payments.bank-transfer.reservation-hours=24"
})
@ActiveProfiles("local")
@Transactional
class BankTransferWorkflowIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired BankTransferWorkflowService workflow;
    @Autowired BankTransferExpiryService expiryService;
    @Autowired AdminBankTransferService adminService;

    @Test
    void adminConfirmationSettlesExactlyOnceAndAssignsFounderPosition() {
        isolateRewardProgram();
        UUID customerId = user("CUSTOMER");
        UUID adminId = user("ADMIN");
        String requestReference = cartRequest(customerId);

        BankTransferResponse created = workflow.create(customerId, requestReference, "bank-checkout-1");
        BankTransferResponse duplicate = workflow.create(customerId, requestReference, "second-browser-tab");

        assertEquals(created.paymentId(), duplicate.paymentId());
        assertEquals(created.expiresAt(), duplicate.expiresAt());
        assertEquals("CREATED", created.status());
        assertEquals("Test Bank", created.bankName());
        assertTrue(created.expiresAt().isAfter(OffsetDateTime.now().plusHours(23)));
        assertEquals(1L, count("SELECT COUNT(*) FROM system_sale_reservations WHERE sales_order_id = ?", created.orderId()));

        BankTransferResponse reported = workflow.reportTransferred(customerId, created.paymentId());
        assertEquals("PROCESSING", reported.status());
        assertNotNull(reported.customerReportedAt());

        OffsetDateTime receivedAt = OffsetDateTime.now().minusMinutes(10);
        BankTransferResponse confirmed = workflow.confirm(adminId, created.paymentId(), "ANZ-TEST-0001", receivedAt, "Matched exact amount");
        BankTransferResponse replay = workflow.confirm(adminId, created.paymentId(), "ANZ-TEST-0001", receivedAt, "Matched exact amount");

        assertEquals("SUCCEEDED", confirmed.status());
        assertEquals("JOINED", confirmed.rewardState());
        assertNotNull(confirmed.reward());
        assertEquals(1, confirmed.reward().founderNumber());
        assertEquals(confirmed.invoiceId(), replay.invoiceId());
        assertEquals(1L, count("SELECT COUNT(*) FROM payments WHERE order_id = ? AND status = 'SUCCEEDED'", created.orderId()));
        assertEquals(1L, count("SELECT COUNT(*) FROM reward_commitments WHERE order_id = ?", created.orderId()));
        assertEquals(1L, count("SELECT COUNT(*) FROM sales_invoices WHERE sales_order_id = ?", created.orderId()));
        assertEquals(1L, count("SELECT COUNT(*) FROM system_sale_allocation_items WHERE sales_order_id = ?", created.orderId()));
        assertEquals(0L, count("SELECT COUNT(*) FROM system_sale_reservations WHERE sales_order_id = ?", created.orderId()));
    }

    @Test
    void reviewQueueUsesCustomerReportedOrderNotCreationOrder() {
        UUID adminId = user("ADMIN");
        UUID firstCustomer = user("CUSTOMER");
        UUID secondCustomer = user("CUSTOMER");
        BankTransferResponse first = workflow.create(firstCustomer, cartRequest(firstCustomer), "first");
        BankTransferResponse second = workflow.create(secondCustomer, cartRequest(secondCustomer), "second");
        workflow.reportTransferred(firstCustomer, first.paymentId());
        workflow.reportTransferred(secondCustomer, second.paymentId());
        jdbc.update("UPDATE payments SET customer_reported_at = CURRENT_TIMESTAMP - INTERVAL '5 minutes' WHERE id = ?", second.paymentId());

        List<AdminBankTransferReviewResponse> queue = adminService.reviewQueue();
        int secondPosition = indexOf(queue, second.paymentId());
        int firstPosition = indexOf(queue, first.paymentId());
        assertTrue(secondPosition >= 0 && firstPosition >= 0 && secondPosition < firstPosition);

        workflow.confirm(adminId, second.paymentId(), "ANZ-TEST-0002", OffsetDateTime.now().minusMinutes(5), null);
        assertFalse(adminService.reviewQueue().stream().anyMatch(item -> item.paymentId().equals(second.paymentId())));
    }

    @Test
    void confirmationRejectsAReceiptTimeEarlierThanTheLatestConfirmedTransfer() {
        isolateRewardProgram();
        UUID adminId = user("ADMIN");
        UUID firstCustomer = user("CUSTOMER");
        UUID secondCustomer = user("CUSTOMER");
        BankTransferResponse first = workflow.create(firstCustomer, cartRequest(firstCustomer), "bank-order-1");
        BankTransferResponse second = workflow.create(secondCustomer, cartRequest(secondCustomer), "bank-order-2");
        workflow.reportTransferred(firstCustomer, first.paymentId());
        workflow.reportTransferred(secondCustomer, second.paymentId());

        workflow.confirm(adminId, first.paymentId(), "ANZ-ORDER-0001", OffsetDateTime.now().minusMinutes(5), null);

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                workflow.confirm(adminId, second.paymentId(), "ANZ-ORDER-0002", OffsetDateTime.now().minusMinutes(10), null));
        assertTrue(error.getMessage().contains("earlier ANZ receipts first"));
        assertEquals("PROCESSING", value("SELECT status FROM payments WHERE id = ?", second.paymentId()));
    }

    @Test
    void adminCanRejectAnUnmatchedReportWithoutCancellingTheOrder() {
        UUID customerId = user("CUSTOMER");
        UUID adminId = user("ADMIN");
        String requestReference = cartRequest(customerId);
        BankTransferResponse payment = workflow.create(customerId, requestReference, "unmatched");
        workflow.reportTransferred(customerId, payment.paymentId());

        BankTransferResponse rejected = workflow.reject(adminId, payment.paymentId(), "No matching amount or reference in ANZ");

        assertEquals("FAILED", rejected.status());
        assertEquals("PENDING_PAYMENT", value("SELECT status FROM sales_orders WHERE id = ?", payment.orderId()));
        assertEquals(0L, count("SELECT COUNT(*) FROM system_sale_reservations WHERE sales_order_id = ?", payment.orderId()));
        BankTransferResponse retry = workflow.create(customerId, requestReference, "retry-unmatched");
        assertEquals("CREATED", retry.status());
    }

    @Test
    void expiredUnreportedTransferReleasesReservationAndAllowsANewAttempt() {
        UUID customerId = user("CUSTOMER");
        String requestReference = cartRequest(customerId);
        BankTransferResponse payment = workflow.create(customerId, requestReference, "expires");
        jdbc.update("UPDATE payments SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id = ?", payment.paymentId());
        expiryService.expireDue();

        assertEquals("CANCELLED", value("SELECT status FROM payments WHERE id = ?", payment.paymentId()));
        assertEquals("PENDING_PAYMENT", value("SELECT status FROM sales_orders WHERE id = ?", payment.orderId()));
        assertEquals(0L, count("SELECT COUNT(*) FROM system_sale_reservations WHERE sales_order_id = ?", payment.orderId()));
        assertThrows(IllegalStateException.class, () -> workflow.reportTransferred(customerId, payment.paymentId()));
        BankTransferResponse retry = workflow.create(customerId, requestReference, "retry");
        assertNotEquals(payment.paymentId(), retry.paymentId());
        assertEquals("CREATED", retry.status());
    }

    @Test
    void aReportedTransferStaysReservedAndRepeatedReportsDoNotCreateMoreReviewItems() {
        UUID customerId = user("CUSTOMER");
        String requestReference = cartRequest(customerId);
        BankTransferResponse payment = workflow.create(customerId, requestReference, "reported-hold");

        BankTransferResponse reported = workflow.reportTransferred(customerId, payment.paymentId());
        BankTransferResponse repeated = workflow.reportTransferred(customerId, payment.paymentId());
        jdbc.update("UPDATE payments SET expires_at = CURRENT_TIMESTAMP - INTERVAL '1 minute' WHERE id = ?", payment.paymentId());
        expiryService.expireDue();

        assertEquals("PROCESSING", value("SELECT status FROM payments WHERE id = ?", payment.paymentId()));
        assertEquals(reported.customerReportedAt(), repeated.customerReportedAt());
        assertEquals(1L, count("SELECT COUNT(*) FROM system_sale_reservations WHERE sales_order_id = ?", payment.orderId()));
        assertEquals(1L, adminService.reviewQueue().stream()
                .filter(item -> item.paymentId().equals(payment.paymentId())).count());
    }

    private UUID user(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name, role, email_verified_at) VALUES (?, ?, 'hash', 'Bank Test', ?, CURRENT_TIMESTAMP)", id, id + "@example.com", role);
        return id;
    }

    private String cartRequest(UUID userId) {
        UUID requestId = UUID.randomUUID();
        String reference = "REQ-" + requestId.toString().substring(0, 12);
        UUID systemId = UUID.fromString("20000000-0000-0000-0000-000000000021");
        jdbc.update("""
                INSERT INTO build_requests
                    (id, user_id, request_reference, name, email, location, direction, estimated_price,
                     recommended_baseline, configuration_snapshot)
                VALUES (?, ?, ?, 'Bank Test', ?, 'Melbourne', 'ai', 1299, 1299,
                    '{"answers":{"cartCheckout":"true"}}'::jsonb)
                """, requestId, userId, reference, userId + "@example.com");
        jdbc.update("""
                INSERT INTO build_request_system_items
                    (id, build_request_id, system_build_id, sku_snapshot, name_snapshot, unit_price_cents, quantity, line_total_cents)
                VALUES (?, ?, ?, 'JON-EDGE-MINI', 'Edge Mini', 129900, 1, 129900)
                """, UUID.randomUUID(), requestId, systemId);
        jdbc.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Bank Test', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, requestId);
        return reference;
    }

    private void isolateRewardProgram() {
        jdbc.query("SELECT id FROM reward_programs WHERE code = 'JON_FOUNDERS_CASHBACK'", rs -> {
            if (!rs.next()) return null;
            UUID programId = rs.getObject("id", UUID.class);
            jdbc.update("DELETE FROM reward_commitments WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_allocations WHERE contribution_id IN (SELECT id FROM reward_contributions WHERE program_id = ?) OR recipient_queue_entry_id IN (SELECT id FROM reward_queue_entries WHERE program_id = ?)", programId, programId);
            jdbc.update("DELETE FROM reward_contributions WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_inbox_events WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_queue_entries WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_policy_versions WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_founder_tiers WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_programs WHERE id = ?", programId);
            return null;
        });
    }

    private long count(String sql, UUID id) { return jdbc.queryForObject(sql, Long.class, id); }
    private String value(String sql, UUID id) { return jdbc.queryForObject(sql, String.class, id); }
    private int indexOf(List<AdminBankTransferReviewResponse> items, UUID paymentId) {
        for (int index = 0; index < items.size(); index++) if (items.get(index).paymentId().equals(paymentId)) return index;
        return -1;
    }
}
