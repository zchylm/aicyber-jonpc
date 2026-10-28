package com.aicyber.backend.cart;

import com.aicyber.backend.cart.dto.CartPreparationRequest;
import com.aicyber.backend.cart.dto.CartPreparationResponse;
import com.aicyber.backend.catalog.SystemCatalogService;
import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.service.InvoiceService;
import com.aicyber.backend.order.service.OrderHistoryService;
import com.aicyber.backend.payment.dto.MockPaymentResponse;
import com.aicyber.backend.payment.service.MockPaymentWorkflowService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class CartCheckoutIntegrationTest {
    @Autowired CartCheckoutService cart;
    @Autowired MockPaymentWorkflowService payments;
    @Autowired InvoiceService invoices;
    @Autowired OrderHistoryService orderHistory;
    @Autowired SystemCatalogService catalog;
    @Autowired JdbcTemplate jdbc;

    @Test
    void localDemoAllowsCartFlowWithoutRealStock() {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, display_name, email_verified_at)
                VALUES (?, ?, 'hash', 'Local Demo Customer', CURRENT_TIMESTAMP)
                """, userId, userId + "@example.com");
        UUID systemId = system("JON-STK-5060");
        jdbc.update("UPDATE system_builds SET sale_enabled = FALSE WHERE id = ?", systemId);
        jdbc.update("UPDATE system_inventory_balances SET on_hand_quantity = 0 WHERE system_build_id = ?", systemId);

        var preview = catalog.previews().stream().filter(item -> item.id().equals(systemId)).findFirst().orElseThrow();
        assertEquals(true, preview.saleEnabled());
        assertEquals(20, preview.availableQuantity());

        CartPreparationResponse prepared = cart.prepare(userId,
                new CartPreparationRequest(List.of(new CartPreparationRequest.Item(systemId, 1))));
        assertNotNull(prepared.requestReference());
        assertEquals(preview.priceCents(), prepared.totalCents());
    }

    @Test
    void twoStockedSystemsBecomeOnePaidOrderWithTwoInvoiceLines() {
        UUID userId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO users (id, email, password_hash, display_name, email_verified_at)
                VALUES (?, ?, 'hash', 'Cart Customer', CURRENT_TIMESTAMP)
                """, userId, userId + "@example.com");
        UUID desktopId = system("JON-STK-5060");
        UUID miniId = system("JON-EDGE-MINI");
        enableStock(desktopId, 3);
        enableStock(miniId, 3);

        CartPreparationResponse prepared = cart.prepare(userId, new CartPreparationRequest(List.of(
                new CartPreparationRequest.Item(desktopId, 1),
                new CartPreparationRequest.Item(miniId, 2)
        )));
        UUID requestId = jdbc.queryForObject(
                "SELECT id FROM build_requests WHERE request_reference = ?", UUID.class, prepared.requestReference());
        assertNotNull(requestId);
        jdbc.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Cart Customer', '0412345678', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, requestId);

        MockPaymentResponse created = payments.create(userId, prepared.requestReference(), UUID.randomUUID().toString());
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_sale_reservations WHERE sales_order_id = ?", Integer.class, created.orderId()));
        MockPaymentResponse paid = payments.complete(userId, created.paymentId(), "SUCCEEDED");

        SalesInvoice invoice = invoices.invoice(paid.invoiceId());
        long founderDiscount = (prepared.totalCents() * 500 + 5_000) / 10_000;
        long discountedTotal = prepared.totalCents() - founderDiscount + 2_500;
        assertEquals(discountedTotal, paid.amountCents());
        assertEquals(4, invoice.lines().size());
        assertEquals(discountedTotal, invoice.lines().stream().mapToLong(line -> line.lineTotalCents()).sum());
        assertEquals(-founderDiscount, invoice.lines().stream()
                .filter(line -> "FOUNDER-5-OFF".equals(line.sku()))
                .findFirst().orElseThrow().lineTotalCents());
        assertEquals(2, jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_sale_allocation_items WHERE sales_order_id = ?", Integer.class, created.orderId()));
        assertEquals(3, jdbc.queryForObject(
                "SELECT SUM(quantity) FROM system_sale_allocation_items WHERE sales_order_id = ?", Integer.class, created.orderId()));
        assertEquals(0, jdbc.queryForObject(
                "SELECT COUNT(*) FROM system_sale_reservations WHERE sales_order_id = ?", Integer.class, created.orderId()));
        assertEquals(2, orderHistory.list(userId).get(0).items().size());
    }

    private UUID system(String sku) {
        return jdbc.queryForObject("SELECT id FROM system_builds WHERE code = ?", UUID.class, sku);
    }

    private void enableStock(UUID id, int quantity) {
        jdbc.update("UPDATE system_builds SET sale_enabled = TRUE WHERE id = ?", id);
        jdbc.update("UPDATE system_inventory_balances SET on_hand_quantity = ? WHERE system_build_id = ?", quantity, id);
    }
}
