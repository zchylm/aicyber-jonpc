package com.aicyber.backend.payment.service;

import com.aicyber.backend.payment.dto.MockPaymentResponse;
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

        MockPaymentResponse completed = service.complete(userId, checkout.paymentId(), "SUCCEEDED");
        MockPaymentResponse replayed = service.complete(userId, checkout.paymentId(), "SUCCEEDED");
        MockPaymentResponse secondTabCompleted = service.complete(userId, secondTabCheckout.paymentId(), "SUCCEEDED");

        assertEquals("SUCCEEDED", completed.status());
        assertEquals("JOINED", completed.rewardState());
        assertNotNull(completed.reward());
        assertEquals(234_700, completed.reward().targetAmountCents());
        assertEquals(completed.reward().id(), replayed.reward().id());
        assertEquals(completed.paymentId(), secondTabCompleted.paymentId());
        assertEquals(1L, count("SELECT COUNT(*) FROM payments p JOIN sales_orders s ON s.id = p.order_id " +
                "WHERE s.user_id = ? AND p.status = 'SUCCEEDED'", userId));
        assertEquals(1L, count("SELECT COUNT(*) FROM reward_queue_entries q " +
                "JOIN sales_orders s ON s.id = q.order_id WHERE s.user_id = ?", userId));
        assertEquals(1L, count("SELECT COUNT(*) FROM reward_inbox_events e " +
                "JOIN sales_orders s ON s.id = e.order_id WHERE s.user_id = ?", userId));
    }

    @Test
    void declinedPaymentDoesNotJoinAndASecondAttemptCanSucceed() {
        isolateRewardProgram();
        UUID userId = createUser();
        String requestReference = createBuildRequest(userId, 1_697);

        MockPaymentResponse firstAttempt = service.create(userId, requestReference, "attempt-1");
        MockPaymentResponse declined = service.complete(userId, firstAttempt.paymentId(), "FAILED");

        assertEquals("FAILED", declined.status());
        assertEquals(0L, count("SELECT COUNT(*) FROM reward_queue_entries q " +
                "JOIN sales_orders s ON s.id = q.order_id WHERE s.user_id = ?", userId));

        MockPaymentResponse secondAttempt = service.create(userId, requestReference, "attempt-2");
        MockPaymentResponse completed = service.complete(userId, secondAttempt.paymentId(), "SUCCEEDED");

        assertEquals("JOINED", completed.rewardState());
        assertEquals(2L, count("SELECT COUNT(*) FROM payments p " +
                "JOIN sales_orders s ON s.id = p.order_id WHERE s.user_id = ?", userId));
        assertEquals(1L, count("SELECT COUNT(*) FROM reward_queue_entries q " +
                "JOIN sales_orders s ON s.id = q.order_id WHERE s.user_id = ?", userId));
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

    private void isolateRewardProgram() {
        jdbcTemplate.query(
                "SELECT id FROM reward_programs WHERE code = 'JON_QUEUE_REWARDS'",
                resultSet -> {
                    if (!resultSet.next()) return null;
                    UUID programId = resultSet.getObject("id", UUID.class);
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
                    jdbcTemplate.update("DELETE FROM reward_programs WHERE id = ?", programId);
                    return null;
                }
        );
    }

    private UUID createUser() {
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
                        "VALUES (?, ?, ?, 'Payment User', ?, 'Melbourne', 'gaming', ?, ?, '{}'::jsonb)",
                requestId,
                userId,
                reference,
                userId + "@example.com",
                price,
                price
        );
        return reference;
    }

    private long count(String sql, UUID userId) {
        return jdbcTemplate.queryForObject(sql, Long.class, userId);
    }
}
