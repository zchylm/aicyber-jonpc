package com.aicyber.backend.payment.service;

import com.aicyber.backend.payment.dto.StripePaymentResponse;
import com.aicyber.backend.payment.stripe.StripeCheckoutGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class StripePaymentWorkflowServiceIntegrationTest {
    @Autowired
    private StripePaymentWorkflowService service;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private StripeCheckoutGateway stripe;

    @Test
    void verifiedWebhookSettlesExactlyOnceUsingServerOrderAmount() {
        isolateRewardProgram();
        UUID userId = createUser();
        String requestReference = createBuildRequest(userId, 3_299);
        when(stripe.create(any(), anyString(), anyLong())).thenReturn(
                new StripeCheckoutGateway.CheckoutSession("cs_test_jonpc", "https://checkout.stripe.test/session", "unpaid", null)
        );

        StripePaymentResponse checkout = service.create(userId, requestReference, "stripe-attempt-1");

        assertEquals(329_900L, checkout.amountCents());
        assertEquals("CREATED", checkout.status());
        assertEquals("https://checkout.stripe.test/session", checkout.checkoutUrl());
        assertNotNull(checkout.rewardPreview());

        StripeCheckoutGateway.CheckoutEvent paid = new StripeCheckoutGateway.CheckoutEvent(
                "evt_test_paid", "checkout.session.completed", "cs_test_jonpc", "paid",
                "pi_test_jonpc", 329_900L, "aud"
        );
        when(stripe.parseWebhook("payload", "signature")).thenReturn(paid);

        service.handleWebhook("payload", "signature");
        service.handleWebhook("payload", "signature");

        assertEquals("SUCCEEDED", value("SELECT status FROM payments WHERE id = ?", checkout.paymentId(), String.class));
        assertEquals("pi_test_jonpc", value("SELECT provider_payment_id FROM payments WHERE id = ?", checkout.paymentId(), String.class));
        assertEquals(1L, count("SELECT COUNT(*) FROM sales_invoices WHERE sales_order_id = ?", checkout.orderId()));
        assertEquals(1L, count("SELECT COUNT(*) FROM reward_commitments WHERE order_id = ?", checkout.orderId()));
    }

    @Test
    void mismatchedWebhookAmountCannotConfirmOrder() {
        isolateRewardProgram();
        UUID userId = createUser();
        String requestReference = createBuildRequest(userId, 1_899);
        when(stripe.create(any(), anyString(), anyLong())).thenReturn(
                new StripeCheckoutGateway.CheckoutSession("cs_test_wrong_amount", "https://checkout.stripe.test/session", "unpaid", null)
        );
        StripePaymentResponse checkout = service.create(userId, requestReference, "stripe-attempt-2");
        when(stripe.parseWebhook("payload", "signature")).thenReturn(new StripeCheckoutGateway.CheckoutEvent(
                "evt_test_wrong", "checkout.session.completed", "cs_test_wrong_amount", "paid",
                "pi_test_wrong", 100L, "aud"
        ));

        IllegalStateException exception = assertThrows(IllegalStateException.class,
                () -> service.handleWebhook("payload", "signature"));

        assertEquals("Stripe payment amount does not match the order", exception.getMessage());
        assertEquals("CREATED", value("SELECT status FROM payments WHERE id = ?", checkout.paymentId(), String.class));
        assertEquals(0L, count("SELECT COUNT(*) FROM sales_invoices WHERE sales_order_id = ?", checkout.orderId()));
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users (id, email, password_hash, display_name, email_verified_at) " +
                        "VALUES (?, ?, 'test-password-hash', 'Stripe Test User', CURRENT_TIMESTAMP)",
                userId, userId + "@example.com"
        );
        return userId;
    }

    private String createBuildRequest(UUID userId, int price) {
        UUID requestId = UUID.randomUUID();
        String reference = "STRIPE-" + requestId.toString().substring(0, 12);
        jdbc.update(
                "INSERT INTO build_requests " +
                        "(id, user_id, request_reference, name, email, location, direction, estimated_price, " +
                        "recommended_baseline, configuration_snapshot) " +
                        "VALUES (?, ?, ?, 'Stripe User', ?, 'Melbourne', 'gaming', ?, ?, " +
                        "'{\"answers\":{\"systemSku\":\"TEST-STRIPE\"}}'::jsonb)",
                requestId, userId, reference, userId + "@example.com", price, price
        );
        jdbc.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Stripe User', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, requestId);
        return reference;
    }

    private void isolateRewardProgram() {
        jdbc.query("SELECT id FROM reward_programs WHERE code = 'JON_FOUNDERS_CASHBACK'", resultSet -> {
            if (!resultSet.next()) return null;
            UUID programId = resultSet.getObject("id", UUID.class);
            jdbc.update("DELETE FROM reward_commitments WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_allocations WHERE contribution_id IN " +
                    "(SELECT id FROM reward_contributions WHERE program_id = ?) OR recipient_queue_entry_id IN " +
                    "(SELECT id FROM reward_queue_entries WHERE program_id = ?)", programId, programId);
            jdbc.update("DELETE FROM reward_contributions WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_inbox_events WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_queue_entries WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_policy_versions WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_founder_tiers WHERE program_id = ?", programId);
            jdbc.update("DELETE FROM reward_programs WHERE id = ?", programId);
            return null;
        });
    }

    private long count(String sql, UUID id) {
        return jdbc.queryForObject(sql, Long.class, id);
    }

    private <T> T value(String sql, UUID id, Class<T> type) {
        return jdbc.queryForObject(sql, type, id);
    }
}
