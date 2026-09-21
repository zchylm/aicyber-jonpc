package com.aicyber.backend.admin;

import com.aicyber.backend.admin.repository.AdminQueryRepository;
import com.aicyber.backend.admin.service.CustomBuildReviewService;
import com.aicyber.backend.payment.service.PaymentService;
import com.aicyber.backend.order.controller.DeliveryDetailsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class CustomBuildReviewIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired AdminQueryRepository adminQueries;
    @Autowired CustomBuildReviewService reviews;
    @Autowired PaymentService payments;
    @Autowired DeliveryDetailsController addresses;

    @Test
    void customEstimateCannotBecomePaymentUntilReviewedAcceptedAndDeliveryConfirmed() {
        UUID customer = user("CUSTOMER");
        UUID reviewer = user("ADMIN");
        UUID requestId = UUID.randomUUID();
        String reference = "REQ-" + requestId;
        request(customer, requestId, reference);

        assertThrows(IllegalStateException.class,
                () -> payments.createMockCheckout(customer, reference, UUID.randomUUID().toString()));
        delivery(requestId);
        assertThrows(IllegalArgumentException.class,
                () -> payments.createMockCheckout(customer, reference, UUID.randomUUID().toString()));

        var first = reviews.publish(requestId, reviewer, new CustomBuildReviewService.PublishQuote(249_900, null, null));
        assertEquals("SENT", first.quoteStatus());
        assertEquals(1L, jdbc.queryForObject("""
                SELECT COUNT(*) FROM transactional_email_outbox
                WHERE message_type = 'CUSTOM_QUOTE_READY' AND aggregate_id = ?
                """, Long.class, first.quoteId()));
        assertThrows(IllegalArgumentException.class,
                () -> payments.createMockCheckout(customer, reference, UUID.randomUUID().toString()));

        var revised = reviews.publish(requestId, reviewer, new CustomBuildReviewService.PublishQuote(259_900, "Final quote", null));
        assertEquals(2, revised.quoteVersion());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> reviews.accept(first.quoteId(), customer));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> reviews.accept(revised.quoteId(), reviewer));

        reviews.accept(revised.quoteId(), customer);
        var payment = payments.createMockCheckout(customer, reference, UUID.randomUUID().toString());
        assertEquals(259_900, payment.amountCents());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> reviews.publish(requestId, reviewer, new CustomBuildReviewService.PublishQuote(269_900, null, null)));
    }

    @Test
    void reviewedBuildIsRemovedFromOverviewAttentionCount() {
        UUID customer = user("CUSTOMER");
        UUID reviewer = user("ADMIN");
        UUID requestId = UUID.randomUUID();
        long before = adminQueries.overview().customReviewsNeeded();

        request(customer, requestId, "REQ-" + requestId);
        assertEquals(before + 1, adminQueries.overview().customReviewsNeeded());

        reviews.publish(requestId, reviewer, new CustomBuildReviewService.PublishQuote(249_900, null, null));
        assertEquals(before, adminQueries.overview().customReviewsNeeded());
    }

    @Test
    void deliveryAddressRequiresFullDetailsAndCannotBeWrittenByAnotherAccount() {
        UUID customer = user("CUSTOMER");
        UUID another = user("CUSTOMER");
        UUID requestId = UUID.randomUUID();
        String reference = "REQ-" + requestId;
        request(customer, requestId, reference);
        assertEquals(org.springframework.http.HttpStatus.NO_CONTENT,
                addresses.get(new UsernamePasswordAuthenticationToken(customer.toString(), null), reference).getStatusCode());
        var valid = new DeliveryDetailsController.DeliveryDetails("Test User", "0400000000",
                "1 Test Street", "Unit 2", "Melbourne", "VIC", "3000");
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> addresses.save(new UsernamePasswordAuthenticationToken(another.toString(), null), reference, valid));
        assertThrows(org.springframework.web.server.ResponseStatusException.class,
                () -> addresses.save(new UsernamePasswordAuthenticationToken(customer.toString(), null), reference,
                        new DeliveryDetailsController.DeliveryDetails("Test User", "0400000000", "1 Test Street", null, "Melbourne", "VIC", "300")));
        assertEquals("Unit 2", addresses.save(new UsernamePasswordAuthenticationToken(customer.toString(), null), reference, valid).addressLine2());
        assertEquals(org.springframework.http.HttpStatus.OK,
                addresses.get(new UsernamePasswordAuthenticationToken(customer.toString(), null), reference).getStatusCode());
    }

    private UUID user(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, display_name, role, email_verified_at)
                VALUES (?, ?, 'test-hash', 'Test User', ?, CURRENT_TIMESTAMP)
                """, id, id + "@example.com", role);
        return id;
    }

    private void request(UUID customer, UUID id, String reference) {
        jdbc.update("""
                INSERT INTO build_requests
                    (id, user_id, request_reference, name, email, location, direction,
                     estimated_price, recommended_baseline, configuration_snapshot)
                VALUES (?, ?, ?, 'Test User', ?, 'Online', 'gaming', 1997, 1997, '{}'::jsonb)
                """, id, customer, reference, customer + "@example.com");
    }

    private void delivery(UUID id) {
        jdbc.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Test User', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, id);
    }
}
