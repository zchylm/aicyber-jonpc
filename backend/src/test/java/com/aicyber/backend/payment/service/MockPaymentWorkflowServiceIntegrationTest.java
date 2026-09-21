package com.aicyber.backend.payment.service;

import com.aicyber.backend.payment.dto.MockPaymentResponse;
import com.aicyber.backend.order.repository.OrderHistoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class MockPaymentWorkflowServiceIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockPaymentWorkflowService service;

    @Autowired
    private OrderHistoryRepository orderHistoryRepository;

    @Test
    void successfulPaymentCreatesOneRewardEntryAndIsSafeToReplay() {
        isolateRewardProgram();
        UUID userId = createUser();
        String requestReference = createBuildRequest(userId, 2_347);

        MockPaymentResponse checkout = service.create(userId, requestReference, "checkout-1");
        MockPaymentResponse duplicateCheckout = service.create(userId, requestReference, "checkout-1");
        MockPaymentResponse secondTabCheckout = service.create(userId, requestReference, "checkout-2");

        assertEquals(checkout.paymentId(), duplicateCheckout.paymentId());
        assertEquals(234_700, checkout.amountCents());
        assertEquals("CREATED", checkout.status());
        assertNotNull(checkout.rewardPreview());
        assertEquals(true, checkout.rewardPreview().available());
        assertEquals("Launch Founder", checkout.rewardPreview().tierName());
        assertEquals(32_004L, checkout.rewardPreview().cashbackAmountCents());

        MockPaymentResponse completed = service.complete(userId, checkout.paymentId(), "SUCCEEDED");
        MockPaymentResponse replayed = service.complete(userId, checkout.paymentId(), "SUCCEEDED");
        MockPaymentResponse secondTabCompleted = service.complete(userId, secondTabCheckout.paymentId(), "SUCCEEDED");

        assertEquals("SUCCEEDED", completed.status());
        assertEquals("JOINED", completed.rewardState());
        assertNotNull(completed.reward());
        assertNotNull(completed.invoiceId());
        assertNotNull(completed.invoiceNumber());
        assertEquals(completed.invoiceId(), replayed.invoiceId());
        assertEquals(completed.invoiceNumber(), replayed.invoiceNumber());
        assertEquals(32_004, completed.reward().cashbackAmountCents());
        assertEquals(completed.reward().id(), replayed.reward().id());
        assertEquals(completed.paymentId(), secondTabCompleted.paymentId());
        assertEquals(1L, count("SELECT COUNT(*) FROM payments p JOIN sales_orders s ON s.id = p.order_id " +
                "WHERE s.user_id = ? AND p.status = 'SUCCEEDED'", userId));
        assertEquals(1L, count("SELECT COUNT(*) FROM reward_commitments c " +
                "JOIN sales_orders s ON s.id = c.order_id WHERE s.user_id = ?", userId));
        assertEquals(1L, count("SELECT COUNT(*) FROM reward_inbox_events e " +
                "JOIN sales_orders s ON s.id = e.order_id WHERE s.user_id = ?", userId));
        assertEquals(1L, count("SELECT COUNT(*) FROM sales_invoices i " +
                "JOIN sales_orders s ON s.id = i.sales_order_id WHERE s.user_id = ?", userId));
        assertEquals(1L, count("SELECT COUNT(*) FROM sales_invoice_lines l " +
                "JOIN sales_invoices i ON i.id = l.invoice_id " +
                "JOIN sales_orders s ON s.id = i.sales_order_id WHERE s.user_id = ?", userId));
        assertEquals(1L, count("SELECT COUNT(*) FROM invoice_delivery_attempts d " +
                "JOIN sales_invoices i ON i.id = d.invoice_id " +
                "JOIN sales_orders s ON s.id = i.sales_order_id " +
                "WHERE s.user_id = ? AND d.status = 'SENT'", userId));
        assertEquals(234_700L, value("SELECT i.total_cents FROM sales_invoices i " +
                "JOIN sales_orders s ON s.id = i.sales_order_id WHERE s.user_id = ?", userId));
        assertEquals(21_336L, value("SELECT i.gst_cents FROM sales_invoices i " +
                "JOIN sales_orders s ON s.id = i.sales_order_id WHERE s.user_id = ?", userId));
        assertEquals(213_364L, value("SELECT i.subtotal_ex_gst_cents FROM sales_invoices i " +
                "JOIN sales_orders s ON s.id = i.sales_order_id WHERE s.user_id = ?", userId));
        assertEquals(1L, value("SELECT i.founder_number FROM sales_invoices i " +
                "JOIN sales_orders s ON s.id = i.sales_order_id WHERE s.user_id = ?", userId));
        assertEquals(32_004L, value("SELECT i.founder_cashback_amount_cents FROM sales_invoices i " +
                "JOIN sales_orders s ON s.id = i.sales_order_id WHERE s.user_id = ?", userId));
        assertNotNull(orderHistoryRepository.findAllByUserId(userId).get(0).reward());
        assertEquals(32_004L, orderHistoryRepository.findAllByUserId(userId).get(0).reward().cashbackAmountCents());
    }

    @Test
    void declinedPaymentDoesNotJoinAndASecondAttemptCanSucceed() {
        isolateRewardProgram();
        UUID userId = createUser();
        String requestReference = createBuildRequest(userId, 1_697);

        MockPaymentResponse firstAttempt = service.create(userId, requestReference, "attempt-1");
        MockPaymentResponse declined = service.complete(userId, firstAttempt.paymentId(), "FAILED");

        assertEquals("FAILED", declined.status());
        assertEquals(0L, count("SELECT COUNT(*) FROM sales_invoices i " +
                "JOIN sales_orders s ON s.id = i.sales_order_id WHERE s.user_id = ?", userId));
        assertEquals(0L, count("SELECT COUNT(*) FROM reward_commitments c " +
                "JOIN sales_orders s ON s.id = c.order_id WHERE s.user_id = ?", userId));

        MockPaymentResponse secondAttempt = service.create(userId, requestReference, "attempt-2");
        MockPaymentResponse completed = service.complete(userId, secondAttempt.paymentId(), "SUCCEEDED");

        assertEquals("JOINED", completed.rewardState());
        assertEquals(2L, count("SELECT COUNT(*) FROM payments p " +
                "JOIN sales_orders s ON s.id = p.order_id WHERE s.user_id = ?", userId));
        assertEquals(1L, count("SELECT COUNT(*) FROM reward_commitments c " +
                "JOIN sales_orders s ON s.id = c.order_id WHERE s.user_id = ?", userId));
    }

    @Test
    void anotherUserCannotCreateOrCompleteThePayment() {
        isolateRewardProgram();
        UUID ownerId = createUser();
        UUID otherUserId = createUser();
        String requestReference = createBuildRequest(ownerId, 2_000);

        assertThrows(IllegalArgumentException.class,
                () -> service.create(otherUserId, requestReference, "other-user"));

        MockPaymentResponse checkout = service.create(ownerId, requestReference, "owner");
        assertThrows(IllegalArgumentException.class,
                () -> service.complete(otherUserId, checkout.paymentId(), "SUCCEEDED"));
    }

    @Test
    void unverifiedUserCannotCreateCheckout() {
        UUID userId = createUnverifiedUser();
        String requestReference = createBuildRequest(userId, 2_000);

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.create(userId, requestReference, "unverified-user"));

        assertEquals("Verify your email before checkout", exception.getMessage());
        assertEquals(0L, count("SELECT COUNT(*) FROM payments p JOIN sales_orders s ON s.id = p.order_id " +
                "WHERE s.user_id = ?", userId));
    }

    private void isolateRewardProgram() {
        jdbcTemplate.query(
                "SELECT id FROM reward_programs WHERE code = 'JON_FOUNDERS_CASHBACK'",
                resultSet -> {
                    if (!resultSet.next()) return null;
                    UUID programId = resultSet.getObject("id", UUID.class);
                    jdbcTemplate.update("DELETE FROM reward_commitments WHERE program_id = ?", programId);
                    jdbcTemplate.update(
                            "DELETE FROM reward_allocations WHERE contribution_id IN " +
                                    "(SELECT id FROM reward_contributions WHERE program_id = ?) " +
                                    "OR recipient_queue_entry_id IN " +
                                    "(SELECT id FROM reward_queue_entries WHERE program_id = ?)",
                            programId, programId
                    );
                    jdbcTemplate.update("DELETE FROM reward_contributions WHERE program_id = ?", programId);
                    jdbcTemplate.update("DELETE FROM reward_inbox_events WHERE program_id = ?", programId);
                    jdbcTemplate.update("DELETE FROM reward_queue_entries WHERE program_id = ?", programId);
                    jdbcTemplate.update("DELETE FROM reward_policy_versions WHERE program_id = ?", programId);
                    jdbcTemplate.update("DELETE FROM reward_founder_tiers WHERE program_id = ?", programId);
                    jdbcTemplate.update("DELETE FROM reward_programs WHERE id = ?", programId);
                    return null;
                }
        );
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name, email_verified_at) " +
                        "VALUES (?, ?, 'test-password-hash', 'Payment Test User', CURRENT_TIMESTAMP)",
                userId,
                userId + "@example.com"
        );
        return userId;
    }

    private UUID createUnverifiedUser() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'test-password-hash', 'Payment Test User')",
                userId,
                userId + "@example.com"
        );
        return userId;
    }

    private String createBuildRequest(UUID userId, int price) {
        UUID requestId = UUID.randomUUID();
        String reference = "REQ-" + requestId.toString().substring(0, 12);
        jdbcTemplate.update(
                "INSERT INTO build_requests " +
                        "(id, user_id, request_reference, name, email, location, direction, estimated_price, " +
                        "recommended_baseline, configuration_snapshot) " +
                        "VALUES (?, ?, ?, 'Payment User', ?, 'Melbourne', 'gaming', ?, ?, '{\"answers\":{\"systemSku\":\"TEST-DEMO\"}}'::jsonb)",
                requestId,
                userId,
                reference,
                userId + "@example.com",
                price,
                price
        );
        jdbcTemplate.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Payment User', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, requestId);
        return reference;
    }

    private long count(String sql, UUID userId) {
        return jdbcTemplate.queryForObject(sql, Long.class, userId);
    }

    private long value(String sql, UUID userId) {
        return jdbcTemplate.queryForObject(sql, Long.class, userId);
    }
}
