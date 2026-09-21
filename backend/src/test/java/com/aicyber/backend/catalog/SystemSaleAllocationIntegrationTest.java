package com.aicyber.backend.catalog;

import com.aicyber.backend.payment.dto.MockPaymentResponse;
import com.aicyber.backend.payment.service.MockPaymentWorkflowService;
import com.aicyber.backend.reward.service.RewardDemoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class SystemSaleAllocationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired SystemCatalogService catalog;
    @Autowired MockPaymentWorkflowService payments;
    @Autowired RewardDemoService demo;
    @Autowired LocalSystemRequestController localRequests;

    @Test
    void paidFeaturedSystemReducesAvailableOnceButFailedAndUnpaidDoNot() {
        SystemProduct system = system("JON-STK-5060");
        UUID user = user();
        int start = system.availableQuantity();
        MockPaymentResponse first = payments.create(user, request(user, system), "first");
        assertEquals(start, system(system.sku()).availableQuantity());
        payments.complete(user, first.paymentId(), "SUCCEEDED");
        assertEquals(start - 1, system(system.sku()).availableQuantity());
        payments.complete(user, first.paymentId(), "SUCCEEDED");
        assertEquals(start - 1, system(system.sku()).availableQuantity());

        MockPaymentResponse second = payments.create(user, request(user, system), "second");
        payments.complete(user, second.paymentId(), "FAILED");
        assertEquals(start - 1, system(system.sku()).availableQuantity());
        request(user, system); // Pay later: no checkout or successful payment.
        assertEquals(start - 1, system(system.sku()).availableQuantity());
    }

    @Test
    void finalUnitCannotBePaidTwiceAndPreviewReportsSoldOut() {
        SystemProduct system = system("JON-STM-5070TI");
        setOnHand(system.id(), 1);
        UUID user = user();
        MockPaymentResponse first = payments.create(user, request(user, system), "last-one");
        MockPaymentResponse second = payments.create(user, request(user, system), "too-late");
        payments.complete(user, first.paymentId(), "SUCCEEDED");
        assertEquals(0, system(system.sku()).availableQuantity());
        assertThrows(IllegalStateException.class, () -> payments.complete(user, second.paymentId(), "SUCCEEDED"));
        assertEquals(0, system(system.sku()).availableQuantity());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM system_sale_allocations WHERE system_build_id = ?", Integer.class, system.id()));
    }

    @Test
    void localResetRestoresTheTestSaleableLimit() {
        SystemProduct system = system("JON-STK-5060TI");
        UUID user = user();
        setOnHand(system.id(), 1);
        MockPaymentResponse payment = payments.create(user, request(user, system), "reset-stock");
        payments.complete(user, payment.paymentId(), "SUCCEEDED");
        assertEquals(0, system(system.sku()).availableQuantity());
        demo.reset(user, "RESET_LOCAL_REWARD_DEMO");
        assertEquals(8, system(system.sku()).availableQuantity());
    }

    @Test
    void soldOutSystemCannotCreateANewLocalRequest() {
        SystemProduct system = system("JON-STM-5070");
        Integer allocated = jdbc.queryForObject("SELECT COUNT(*) FROM system_sale_allocations WHERE system_build_id = ?", Integer.class, system.id());
        setOnHand(system.id(), allocated);
        UUID user = user();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> localRequests.create(new UsernamePasswordAuthenticationToken(user.toString(), null),
                        new LocalSystemRequestController.LocalSystemRequest(system.sku(), system.priceCents())));
        assertEquals(409, error.getStatusCode().value());
        assertEquals(0, system(system.sku()).availableQuantity());
    }

    private SystemProduct system(String sku) {
        return catalog.previews().stream().filter(item -> item.sku().equals(sku)).findFirst().orElseThrow();
    }

    private void setOnHand(UUID systemId, int quantity) {
        jdbc.update("UPDATE system_inventory_balances SET on_hand_quantity = ? WHERE system_build_id = ?", quantity, systemId);
    }

    private UUID user() {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name, email_verified_at) VALUES (?, ?, 'test', 'Stock Test', CURRENT_TIMESTAMP)", id, id + "@example.com");
        return id;
    }

    private String request(UUID user, SystemProduct system) {
        UUID id = UUID.randomUUID();
        String reference = "STOCK-" + id.toString().substring(0, 12);
        String snapshot = "{\"answers\":{\"systemId\":\"" + system.id() + "\",\"systemSku\":\"" + system.sku() + "\"}}";
        jdbc.update("""
                INSERT INTO build_requests (id, user_id, request_reference, name, email, location, direction,
                    estimated_price, recommended_baseline, configuration_snapshot)
                VALUES (?, ?, ?, 'Stock Test', ?, 'Melbourne', 'gaming', ?, ?, ?::jsonb)
                """, id, user, reference, user + "@example.com", Math.toIntExact(system.priceCents() / 100),
                Math.toIntExact(system.priceCents() / 100), snapshot);
        jdbc.update("""
                INSERT INTO build_delivery_details (build_request_id, recipient_name, phone, address_line_1, suburb, state, postcode)
                VALUES (?, 'Stock Test', '0400000000', '1 Test Street', 'Melbourne', 'VIC', '3000')
                """, id);
        return reference;
    }
}
