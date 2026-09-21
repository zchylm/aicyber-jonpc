package com.aicyber.backend.reward.service;

import com.aicyber.backend.payment.dto.MockPaymentResponse;
import com.aicyber.backend.payment.service.MockPaymentWorkflowService;
import com.aicyber.backend.reward.dto.RewardDemoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class RewardDemoServiceIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RewardDemoService demoService;

    @Autowired
    private MockPaymentWorkflowService paymentWorkflow;

    @Test
    void convertsBuildRequestAndLocksCashbackWithoutFutureOrderFunding() {
        isolateDemoProgram();
        UUID userId = createUserWithBuildRequest();

        RewardDemoResponse joined = demoService.qualifyLatestBuildRequest(userId);

        assertEquals("ACTIVE", joined.member().state());
        assertEquals(1, joined.member().entries().size());
        assertEquals(1, joined.member().entries().get(0).founderNumber());
        assertEquals(1500, joined.member().entries().get(0).rateBasisPoints());
        assertEquals(27_272, joined.member().entries().get(0).cashbackAmountCents());

        RewardDemoResponse allocated = demoService.createFounderOrder(userId);

        assertEquals(27_272, allocated.member().entries().get(0).cashbackAmountCents());
        assertEquals(2, allocated.summary().confirmedCount());
        assertEquals(48, allocated.summary().remainingPositions());
        assertTrue(allocated.summary().available());
        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_orders WHERE status = 'REWARD_ELIGIBLE' " +
                        "AND id IN (SELECT order_id FROM reward_commitments WHERE program_id = " +
                        "(SELECT id FROM reward_programs WHERE code = 'JON_FOUNDERS_CASHBACK'))",
                Long.class
        ));
    }

    @Test
    void resetsLocalRewardDataAndOnlyTheCurrentCustomersOrderHistory() {
        isolateDemoProgram();
        UUID userId = createUserWithBuildRequest();
        UUID otherUserId = createUserWithBuildRequest();

        demoService.qualifyLatestBuildRequest(userId);
        demoService.createFounderOrder(userId);

        RewardDemoResponse reset = demoService.reset(userId, "RESET_LOCAL_REWARD_DEMO");

        assertEquals(0, reset.summary().confirmedCount());
        assertEquals(50, reset.summary().remainingPositions());
        assertEquals("NO_ENTRY", reset.member().state());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Long.class,
                userId
        ));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM build_requests WHERE user_id = ?",
                Long.class,
                userId
        ));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_orders WHERE user_id = ?",
                Long.class,
                userId
        ));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM build_requests WHERE user_id = ?",
                Long.class,
                otherUserId
        ));
    }

    @Test
    void resetsInvoicesBeforeTheirPaymentsAndOrders() {
        isolateDemoProgram();
        UUID userId = createUserWithBuildRequest();
        String requestReference = jdbcTemplate.queryForObject(
                "SELECT request_reference FROM build_requests WHERE user_id = ?",
                String.class,
                userId
        );
        UUID requestId = jdbcTemplate.queryForObject("SELECT id FROM build_requests WHERE user_id = ?", UUID.class, userId);
        jdbcTemplate.update("""
                INSERT INTO custom_build_quotes
                    (id, build_request_id, version, status, total_cents, reviewer_id)
                VALUES (?, ?, 1, 'SENT', 200000, ?)
                """, UUID.randomUUID(), requestId, userId);

        MockPaymentResponse checkout = paymentWorkflow.create(userId, requestReference, "reset-invoice-graph");
        MockPaymentResponse paid = paymentWorkflow.complete(userId, checkout.paymentId(), "SUCCEEDED");

        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_invoices WHERE id = ?",
                Long.class,
                paid.invoiceId()
        ));
        assertTrue(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_invoice_lines WHERE invoice_id = ?",
                Long.class,
                paid.invoiceId()
        ) > 0);

        demoService.reset(userId, "RESET_LOCAL_REWARD_DEMO");

        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM custom_build_quotes WHERE build_request_id = ?",
                Long.class, requestId));

        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_invoices WHERE id = ?",
                Long.class,
                paid.invoiceId()
        ));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE id = ?",
                Long.class,
                paid.paymentId()
        ));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sales_orders WHERE id = ?",
                Long.class,
                paid.orderId()
        ));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE id = ?",
                Long.class,
                userId
        ));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM build_requests WHERE user_id = ?",
                Long.class,
                userId
        ));
    }

    @Test
    void advancesTheLocalCashbackFromLockedToPayableToPaid() {
        isolateDemoProgram();
        UUID userId = createUserWithBuildRequest();
        demoService.qualifyLatestBuildRequest(userId);

        RewardDemoResponse payable = demoService.makeLatestCashbackPayable(userId);
        assertEquals("PAYABLE", payable.member().entries().get(0).status());
        assertTrue(payable.member().entries().get(0).payableAt() != null);
        assertTrue(payable.member().entries().get(0).payoutDueAt() != null);

        RewardDemoResponse paid = demoService.markLatestCashbackPaid(userId);
        assertEquals("PAID", paid.member().entries().get(0).status());
        assertTrue(paid.member().entries().get(0).paidAt() != null);
        assertTrue(paid.member().entries().get(0).payoutReference().startsWith("DEMO-PAYOUT-"));
        assertEquals("ORIGINAL_PAYMENT_METHOD", paid.member().entries().get(0).payoutMethod());
    }

    private void isolateDemoProgram() {
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

    private UUID createUserWithBuildRequest() {
        UUID userId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name, email_verified_at) " +
                        "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)",
                userId, userId + "@example.com", "test-password-hash", "Reward Demo Test User"
        );
        jdbcTemplate.update(
                "INSERT INTO build_requests " +
                        "(id, user_id, request_reference, name, email, location, direction, estimated_price, " +
                        "recommended_baseline, configuration_snapshot) " +
                        "VALUES (?, ?, ?, 'Demo User', ?, 'Melbourne', 'gaming', 2000, 2000, '{\"answers\":{\"systemSku\":\"TEST-DEMO\"}}'::jsonb)",
                requestId,
                userId,
                "REQ-" + requestId,
                userId + "@example.com"
        );
        jdbcTemplate.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Demo User', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, requestId);
        return userId;
    }
}
