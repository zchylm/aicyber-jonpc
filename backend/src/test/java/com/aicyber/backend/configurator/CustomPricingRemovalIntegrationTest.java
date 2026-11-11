package com.aicyber.backend.configurator;

import com.aicyber.backend.admin.service.CustomBuildReviewService;
import com.aicyber.backend.build.dto.SaveBuildRequest;
import com.aicyber.backend.build.repository.SavedBuildRepository;
import com.aicyber.backend.configurator.dto.ConfiguratorBuildRequest;
import com.aicyber.backend.configurator.dto.ConfiguratorSelection;
import com.aicyber.backend.configurator.service.ConfiguratorRequestService;
import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.order.repository.OrderHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class CustomPricingRemovalIntegrationTest {
    @Autowired SavedBuildRepository savedBuilds;
    @Autowired ConfiguratorRequestService requests;
    @Autowired CustomBuildReviewService reviews;
    @Autowired SalesOrderService orders;
    @Autowired OrderHistoryRepository history;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;

    @Test
    void savesAndRestoresSelectionsWithoutExposingOrRetainingAnAutomaticPrice() {
        UUID customer = user("CUSTOMER");
        var request = new SaveBuildRequest("My custom build", "gaming", "$2,800–$3,800", selection());
        var saved = savedBuilds.create(customer, request);
        // Existing saved selections can still be read when legacy amount columns contain old estimates.
        jdbc.update("UPDATE saved_builds SET estimated_price = 3397, recommended_baseline = 3300, selected_adjustments = 97 WHERE id = ?", saved.id());
        var restored = savedBuilds.findAllByUserId(customer).get(0);
        assertEquals(selection(), restored.configuration());
        var fields = java.util.Arrays.stream(restored.getClass().getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList();
        for (String field : new String[]{"estimatedPrice", "recommendedBaseline", "selectedAdjustments"})
            assertFalse(fields.contains(field));

        savedBuilds.update(customer, saved.id(), request);
        assertEquals(0, jdbc.queryForObject("SELECT estimated_price + recommended_baseline + selected_adjustments FROM saved_builds WHERE id = ?", Integer.class, saved.id()));
    }

    @Test
    void customRequestHasNoPriceAndCanOnlyBecomeAnOrderAtTheAcceptedStaffQuote() {
        UUID customer = user("CUSTOMER");
        UUID reviewer = user("ADMIN");
        var submitted = requests.submit(new ConfiguratorBuildRequest("Test User", customer + "@example.test",
                "", "Online", "", false, selection()), customer);
        UUID requestId = jdbc.queryForObject("SELECT id FROM build_requests WHERE request_reference = ?", UUID.class, submitted.requestReference());
        assertEquals(0, jdbc.queryForObject("SELECT estimated_price + recommended_baseline + selected_adjustments FROM build_requests WHERE id = ?", Integer.class, requestId));
        assertFalse(json.valueToTree(submitted).has("quote"));
        var pending = history.findAllByUserId(customer).get(0);
        assertNull(pending.productSubtotalCents());
        var historyFields = java.util.Arrays.stream(pending.getClass().getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).toList();
        for (String field : new String[]{"estimatedPrice", "recommendedBaseline", "selectedAdjustments"})
            assertFalse(historyFields.contains(field));
        assertThrows(IllegalArgumentException.class, () -> orders.createFromBuildRequest(customer, submitted.requestReference()));

        var quote = reviews.publish(requestId, reviewer, new CustomBuildReviewService.PublishQuote(318_735, "Staff-reviewed price", null));
        assertTrue(java.util.Arrays.stream(quote.getClass().getRecordComponents())
                .noneMatch(field -> field.getName().equals("estimatedPrice")));
        assertThrows(IllegalArgumentException.class, () -> orders.createFromBuildRequest(customer, submitted.requestReference()));
        reviews.accept(quote.quoteId(), customer);
        var order = orders.createFromBuildRequest(customer, submitted.requestReference());
        assertEquals(318_735L, jdbc.queryForObject("SELECT product_subtotal_cents FROM sales_orders WHERE id = ?", Long.class, order.id()));
        assertEquals(318_735L, jdbc.queryForObject("SELECT amount_cents FROM sales_orders WHERE id = ?", Long.class, order.id()));
        assertEquals(318_735L, history.findAllByUserId(customer).get(0).productSubtotalCents());
    }

    private UUID user(String role) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name, role, email_verified_at) VALUES (?, ?, 'hash', 'Test User', ?, CURRENT_TIMESTAMP)",
                id, id + "@example.test", role);
        return id;
    }

    private ConfiguratorSelection selection() {
        return new ConfiguratorSelection("gaming", Map.of("budget", "$2,800–$3,800"),
                "ryzen-5-9600x", "rtx-5070", "32gb", "2tb", "b850-wifi", "750-gold", "mid", "dual-tower-air",
                "ryzen-5-9600x", "rtx-5070", "32gb", "2tb", "b850-wifi", "750-gold", "mid", "dual-tower-air", "black");
    }
}
