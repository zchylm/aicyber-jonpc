package com.aicyber.backend.order;

import com.aicyber.backend.order.controller.DeliveryDetailsController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class DeliveryDetailsValidationIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired DeliveryDetailsController delivery;

    @Test
    void acceptsLocalAndInternationalAustralianNumbersAndNormalisesThem() {
        Request request = request();
        var auth = new UsernamePasswordAuthenticationToken(request.user().toString(), null);
        var local = delivery.save(auth, request.reference(), details("0412 345 678"));
        assertEquals("+61412345678", local.phone());
        var landline = delivery.save(auth, request.reference(), details("+61 (3) 9123-4567"));
        assertEquals("+61391234567", landline.phone());
        assertEquals("+61391234567", jdbc.queryForObject(
                "SELECT phone FROM build_delivery_details WHERE build_request_id = ?", String.class, request.id()));
    }

    @Test
    void rejectsMalformedAndForeignNumbersOnTheServer() {
        Request request = request();
        var auth = new UsernamePasswordAuthenticationToken(request.user().toString(), null);
        for (String phone : new String[] {"12345", "+14123456789", "041234567", "041234567890"}) {
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> delivery.save(auth, request.reference(), details(phone)));
            assertEquals(400, error.getStatusCode().value());
        }
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM build_delivery_details WHERE build_request_id = ?", Integer.class, request.id()));
    }

    private DeliveryDetailsController.DeliveryDetails details(String phone) {
        return new DeliveryDetailsController.DeliveryDetails("Test Recipient", phone, "1 Test Street", null,
                "Melbourne", "VIC", "3000");
    }

    private Request request() {
        UUID user = UUID.randomUUID();
        UUID id = UUID.randomUUID();
        String reference = "DEL-" + id.toString().substring(0, 12);
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, 'test', 'Delivery Test')", user, user + "@example.com");
        jdbc.update("""
                INSERT INTO build_requests (id, user_id, request_reference, name, email, location, direction,
                    estimated_price, recommended_baseline, configuration_snapshot)
                VALUES (?, ?, ?, 'Delivery Test', ?, 'Melbourne', 'gaming', 2000, 2000, '{}'::jsonb)
                """, id, user, reference, user + "@example.com");
        return new Request(id, user, reference);
    }

    private record Request(UUID id, UUID user, String reference) {}
}
