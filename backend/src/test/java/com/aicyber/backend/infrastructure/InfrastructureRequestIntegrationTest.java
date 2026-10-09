package com.aicyber.backend.infrastructure;

import com.aicyber.backend.admin.service.CustomBuildReviewService;
import com.aicyber.backend.order.controller.DeliveryDetailsController;
import com.aicyber.backend.order.repository.OrderHistoryRepository;
import com.aicyber.backend.payment.service.MockPaymentWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class InfrastructureRequestIntegrationTest {
    @Autowired InfrastructureRequestService requests;
    @Autowired CustomBuildReviewService reviews;
    @Autowired DeliveryDetailsController deliveryDetails;
    @Autowired MockPaymentWorkflowService payments;
    @Autowired OrderHistoryRepository orderHistory;
    @Autowired JdbcTemplate jdbc;

    @Test
    void submittedInfrastructureProjectAppearsInAdminReview() {
        UUID userId = user();
        var response = requests.submit(userId, new InfrastructureRequestService.Request(
                "Example AI", "0400000000", "Melbourne", "Stage one", true, configuration()));

        assertEquals("RECEIVED", response.status());
        var review = reviews.list().stream()
                .filter(item -> item.requestReference().equals(response.requestReference()))
                .findFirst().orElseThrow();
        assertEquals("AI Infrastructure", review.direction());
        assertEquals("Example AI", ((Map<?, ?>) review.configuration().get("answers")).get("Organisation"));
    }

    @Test
    void incompleteInfrastructureProjectIsRejected() {
        UUID userId = user();
        Map<String, String> incomplete = configuration();
        incomplete.remove("Power");
        assertThrows(IllegalArgumentException.class, () -> requests.submit(userId,
                new InfrastructureRequestService.Request("Example AI", null, "Melbourne", null, true, incomplete)));
    }

    @Test
    void infrastructureProjectCompletesReviewQuotePaymentInvoiceAndFounderQueue() {
        UUID customer = user();
        UUID reviewer = admin();
        var submitted = requests.submit(customer, new InfrastructureRequestService.Request(
                "Complete AI", "0400000000", "Melbourne", "Reviewed deployment", true, configuration()));
        var adminReview = reviews.list().stream()
                .filter(item -> item.requestReference().equals(submitted.requestReference()))
                .findFirst().orElseThrow();

        deliveryDetails.save(new UsernamePasswordAuthenticationToken(customer.toString(), null),
                submitted.requestReference(), new DeliveryDetailsController.DeliveryDetails(
                        "Infrastructure User", "0400000000", "1 Test Street", null,
                        "Melbourne", "VIC", "3000"));
        assertThrows(IllegalArgumentException.class,
                () -> payments.create(customer, submitted.requestReference(), "before-quote"));

        var quote = reviews.publish(adminReview.id(), reviewer,
                new CustomBuildReviewService.PublishQuote(3_850_000, "Approved project scope", null));
        assertEquals("SENT", quote.quoteStatus());
        assertThrows(IllegalArgumentException.class,
                () -> payments.create(customer, submitted.requestReference(), "before-acceptance"));

        reviews.accept(quote.quoteId(), customer);
        var checkout = payments.create(customer, submitted.requestReference(), "infrastructure-checkout");
        assertEquals(3_850_000, checkout.amountCents());
        assertEquals("CREATED", checkout.status());
        assertNotNull(checkout.rewardPreview());

        var paid = payments.complete(customer, checkout.paymentId(), "SUCCEEDED");
        assertEquals("SUCCEEDED", paid.status());
        assertEquals("JOINED", paid.rewardState());
        assertNotNull(paid.reward());
        assertNotNull(paid.invoiceId());
        assertNotNull(paid.invoiceNumber());

        var history = orderHistory.findAllByUserId(customer).stream()
                .filter(item -> item.requestReference().equals(submitted.requestReference()))
                .findFirst().orElseThrow();
        assertEquals("AI Infrastructure", history.direction());
        assertEquals("ACCEPTED", history.quoteStatus());
        assertEquals(3_850_000L, history.quoteTotalCents());
        assertEquals("SUCCEEDED", history.paymentStatus());
        assertNotNull(history.reward());
        assertNotNull(history.invoiceId());
        assertTrue(history.configuration().answers().containsKey("Selected services"));
        assertEquals(1L, count("SELECT COUNT(*) FROM transactional_email_outbox WHERE message_type = 'CUSTOM_BUILD_RECEIVED' AND aggregate_id = ?", adminReview.id()));
        assertEquals(1L, count("SELECT COUNT(*) FROM transactional_email_outbox WHERE message_type = 'CUSTOM_QUOTE_READY' AND aggregate_id = ?", quote.quoteId()));
        assertEquals(1L, count("SELECT COUNT(*) FROM reward_commitments WHERE order_id = ?", paid.orderId()));
        assertEquals(1L, count("SELECT COUNT(*) FROM sales_invoices WHERE id = ?", paid.invoiceId()));
        assertFalse(orderHistory.findAllByUserId(customer).isEmpty());
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, display_name, role, email_verified_at)
                VALUES (?, ?, 'test-hash', 'Infrastructure User', 'CUSTOMER', CURRENT_TIMESTAMP)
                """, id, id + "@example.com");
        return id;
    }

    private UUID admin() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, display_name, role, email_verified_at)
                VALUES (?, ?, 'test-hash', 'Infrastructure Admin', 'ADMIN', CURRENT_TIMESTAMP)
                """, id, id + "@example.com");
        return id;
    }

    private long count(String sql, UUID id) {
        return jdbc.queryForObject(sql, Long.class, id);
    }

    private Map<String, String> configuration() {
        Map<String, String> values = new LinkedHashMap<>();
        for (String field : new String[]{"Scale", "Platform", "GPU", "CPU", "Memory", "Storage", "Network",
                "Systems", "System platform", "Power", "Cooling", "Chassis", "Operating system", "Management"}) {
            values.put(field, field + " selection");
        }
        values.put("Selected services", "Engineering review");
        return values;
    }
}
