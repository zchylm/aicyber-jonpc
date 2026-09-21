package com.aicyber.backend.admin;

import com.aicyber.backend.auth.security.JwtService;
import com.aicyber.backend.payment.dto.MockPaymentResponse;
import com.aicyber.backend.payment.service.MockPaymentWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class AdminInvoiceIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MockPaymentWorkflowService paymentWorkflow;

    @Test
    void adminCanFindDownloadAndResendAnInvoiceWithAnAuditTrail() throws Exception {
        Identity customer = createUser("CUSTOMER", "Invoice Customer");
        Identity admin = createUser("ADMIN", "Invoice Admin");
        String requestReference = createBuildRequest(customer.id());
        MockPaymentResponse checkout = paymentWorkflow.create(customer.id(), requestReference, "admin-invoice");
        MockPaymentResponse paid = paymentWorkflow.complete(customer.id(), checkout.paymentId(), "SUCCEEDED");
        String customerReference = jdbcTemplate.queryForObject(
                "SELECT customer_reference FROM users WHERE id = ?", String.class, customer.id());

        mockMvc.perform(get("/api/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.token())
                        .queryParam("query", customerReference))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].customerReference").value(customerReference))
                .andExpect(jsonPath("$.items[0].invoiceNumber").value(paid.invoiceNumber()));

        mockMvc.perform(get("/api/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.token())
                        .queryParam("query", paid.invoiceNumber()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1));

        mockMvc.perform(get("/api/admin/orders/{orderId}", paid.orderId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerReference").value(customerReference))
                .andExpect(jsonPath("$.configuration.answers.systemSku").value("TEST-DEMO"))
                .andExpect(jsonPath("$.configuration.array").doesNotExist())
                .andExpect(jsonPath("$.delivery.recipientName").value("Invoice Customer"))
                .andExpect(jsonPath("$.delivery.phone").value("0400000000"))
                .andExpect(jsonPath("$.delivery.addressLine1").value("1 Test Street"))
                .andExpect(jsonPath("$.delivery.postcode").value("3000"))
                .andExpect(jsonPath("$.invoice.invoiceNumber").value(paid.invoiceNumber()))
                .andExpect(jsonPath("$.invoice.gstCents").value(21336));

        jdbcTemplate.update("UPDATE build_delivery_details SET address_line_1 = 'Changed request address' WHERE build_request_id = (SELECT build_request_id FROM sales_orders WHERE id = ?)", paid.orderId());
        mockMvc.perform(get("/api/admin/orders/{orderId}", paid.orderId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.delivery.addressLine1").value("1 Test Street"));

        mockMvc.perform(get("/api/admin/invoices/{invoiceId}/pdf", paid.invoiceId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment;")));

        mockMvc.perform(post("/api/admin/invoices/{invoiceId}/resend", paid.invoiceId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deliveryStatus").value("SENT"));

        mockMvc.perform(get("/api/admin/invoices/{invoiceId}/pdf", paid.invoiceId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customer.token()))
                .andExpect(status().isForbidden());

        assertCount(2, "SELECT COUNT(*) FROM invoice_delivery_attempts WHERE invoice_id = ?", paid.invoiceId());
        assertCount(1, "SELECT COUNT(*) FROM admin_audit_events WHERE entity_id = ? AND action = 'INVOICE_DOWNLOADED'", paid.invoiceId());
        assertCount(1, "SELECT COUNT(*) FROM admin_audit_events WHERE entity_id = ? AND action = 'INVOICE_RESENT'", paid.invoiceId());
    }

    private Identity createUser(String role, String displayName) {
        UUID id = UUID.randomUUID();
        String email = id + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name, role, email_verified_at) " +
                        "VALUES (?, ?, 'test-password-hash', ?, ?, CURRENT_TIMESTAMP)",
                id, email, displayName, role
        );
        return new Identity(id, jwtService.createToken(id, email, role, 1));
    }

    private String createBuildRequest(UUID userId) {
        UUID requestId = UUID.randomUUID();
        String reference = "REQ-" + requestId.toString().substring(0, 12);
        jdbcTemplate.update(
                "INSERT INTO build_requests " +
                        "(id, user_id, request_reference, name, email, location, direction, estimated_price, " +
                        "recommended_baseline, configuration_snapshot) " +
                        "VALUES (?, ?, ?, 'Invoice Customer', ?, 'Melbourne', 'gaming', 2347, 2347, '{\"answers\":{\"systemSku\":\"TEST-DEMO\"}}'::jsonb)",
                requestId, userId, reference, userId + "@example.com"
        );
        jdbcTemplate.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Invoice Customer', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, requestId);
        return reference;
    }

    private void assertCount(long expected, String sql, UUID invoiceId) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, jdbcTemplate.queryForObject(sql, Long.class, invoiceId));
    }

    private record Identity(UUID id, String token) {
    }
}
