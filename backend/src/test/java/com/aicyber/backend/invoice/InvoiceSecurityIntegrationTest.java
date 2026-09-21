package com.aicyber.backend.invoice;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@Transactional
class InvoiceSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private MockPaymentWorkflowService paymentWorkflow;

    @Test
    void onlyTheOrderOwnerCanReadAndDownloadTheInvoice() throws Exception {
        UUID ownerId = createUser();
        UUID otherUserId = createUser();
        String requestReference = createBuildRequest(ownerId);
        MockPaymentResponse checkout = paymentWorkflow.create(ownerId, requestReference, "invoice-security");
        MockPaymentResponse paid = paymentWorkflow.complete(ownerId, checkout.paymentId(), "SUCCEEDED");

        String ownerToken = token(ownerId);
        String otherToken = token(otherUserId);

        mockMvc.perform(get("/api/invoices/{invoiceId}", paid.invoiceId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoiceNumber").value(paid.invoiceNumber()))
                .andExpect(jsonPath("$.totalCents").value(234700));

        mockMvc.perform(get("/api/invoices/{invoiceId}/pdf", paid.invoiceId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment;")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));

        mockMvc.perform(get("/api/invoices/{invoiceId}", paid.invoiceId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/invoices/{invoiceId}/pdf", paid.invoiceId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/local/invoices/{invoiceId}/pdf", paid.invoiceId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, startsWith("attachment;")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name, email_verified_at) " +
                        "VALUES (?, ?, 'test-password-hash', 'Invoice Test User', CURRENT_TIMESTAMP)",
                userId,
                userId + "@example.com"
        );
        return userId;
    }

    private String createBuildRequest(UUID userId) {
        UUID requestId = UUID.randomUUID();
        String reference = "REQ-" + requestId.toString().substring(0, 12);
        jdbcTemplate.update(
                "INSERT INTO build_requests " +
                        "(id, user_id, request_reference, name, email, location, direction, estimated_price, " +
                        "recommended_baseline, configuration_snapshot) " +
                        "VALUES (?, ?, ?, 'Invoice Buyer', ?, 'Melbourne', 'gaming', 2347, 2347, '{\"answers\":{\"systemSku\":\"TEST-DEMO\"}}'::jsonb)",
                requestId,
                userId,
                reference,
                userId + "@example.com"
        );
        jdbcTemplate.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Invoice Buyer', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, requestId);
        return reference;
    }

    private String token(UUID userId) {
        return jwtService.createToken(userId, userId + "@example.com", "CUSTOMER", 1);
    }
}
