package com.aicyber.backend.catalog;

import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.aicyber.backend.order.repository.OrderHistoryRepository;
import com.aicyber.backend.payment.service.MockPaymentWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class LocalSystemRequestIntegrationTest {
    @Autowired LocalSystemRequestController controller;
    @Autowired SystemCatalogService catalog;
    @Autowired UserRepository users;
    @Autowired OrderHistoryRepository orders;
    @Autowired MockPaymentWorkflowService payments;
    @Autowired JdbcTemplate jdbc;

    @Test
    void selectedDraftCreatesExactPriceSnapshotWithoutChangingStock() {
        SystemProduct system = catalog.previews().get(0);
        UUID userId = UUID.randomUUID();
        users.create(userId, userId + "@example.com", "hash", "Local buyer", OffsetDateTime.now());
        long stockBefore = count("SELECT COALESCE(SUM(on_hand_quantity), 0) FROM inventory_balances");

        var request = controller.create(new UsernamePasswordAuthenticationToken(userId.toString(), null),
                new LocalSystemRequestController.LocalSystemRequest(system.sku(), system.priceCents()));
        assertEquals(system.priceCents(), request.priceCents());
        assertEquals(system.sku(), request.sku());
        ConfiguratorQuoteRequest snapshot = orders.findAllByUserId(userId).get(0).configuration();
        assertEquals(system.sku(), snapshot.answers().get("systemSku"));
        assertEquals(system.name(), snapshot.answers().get("systemName"));
        assertEquals(system.specifications().get("GPU"), snapshot.answers().get("GPU"));

        UUID requestId = jdbc.queryForObject("SELECT id FROM build_requests WHERE request_reference = ?", UUID.class, request.requestReference());
        jdbc.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Local buyer', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, requestId);

        var checkout = payments.create(userId, request.requestReference(), UUID.randomUUID().toString());
        assertEquals(system.priceCents(), checkout.amountCents());
        assertNotNull(checkout.orderReference());
        var settled = payments.complete(userId, checkout.paymentId(), "SUCCEEDED");
        assertNotNull(settled.invoiceId());
        assertEquals(system.sku(), jdbc.queryForObject(
                "SELECT sku_snapshot FROM sales_invoice_lines WHERE invoice_id = ?", String.class, settled.invoiceId()));
        assertEquals(system.name(), jdbc.queryForObject(
                "SELECT description_snapshot FROM sales_invoice_lines WHERE invoice_id = ?", String.class, settled.invoiceId()));
        assertEquals(stockBefore, count("SELECT COALESCE(SUM(on_hand_quantity), 0) FROM inventory_balances"));
        assertEquals(0L, count("SELECT COUNT(*) FROM inventory_reservations"));
    }

    @Test
    void changedPriceIsRejectedBeforeRequestIsSaved() {
        SystemProduct system = catalog.previews().get(0);
        UUID userId = UUID.randomUUID();
        users.create(userId, userId + "@example.com", "hash", "Local buyer", OffsetDateTime.now());
        assertThrows(ResponseStatusException.class, () -> controller.create(
                new UsernamePasswordAuthenticationToken(userId.toString(), null),
                new LocalSystemRequestController.LocalSystemRequest(system.sku(), system.priceCents() - 100)));
        assertEquals(0, orders.findAllByUserId(userId).size());
    }

    private long count(String sql) { return jdbc.queryForObject(sql, Long.class); }
}
