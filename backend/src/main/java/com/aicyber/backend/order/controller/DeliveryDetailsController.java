package com.aicyber.backend.order.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders/delivery")
public class DeliveryDetailsController {
    private final JdbcTemplate jdbc;
    public DeliveryDetailsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/{reference}")
    public ResponseEntity<DeliveryDetails> get(Authentication authentication, @PathVariable String reference) {
        UUID requestId = ownedRequest(authentication, reference);
        return find(requestId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    private java.util.Optional<DeliveryDetails> find(UUID requestId) {
        return jdbc.query("""
                SELECT recipient_name, phone, address_line_1, address_line_2, suburb, state, postcode
                FROM build_delivery_details WHERE build_request_id = ?
                """, (rs, row) -> new DeliveryDetails(rs.getString(1), rs.getString(2), rs.getString(3),
                rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7)), requestId)
                .stream().findFirst();
    }

    @PutMapping("/{reference}")
    @Transactional
    public DeliveryDetails save(Authentication authentication, @PathVariable String reference, @RequestBody DeliveryDetails details) {
        UUID requestId = ownedRequest(authentication, reference);
        if (details == null || invalid(details.recipientName(), 160) || invalid(details.phone(), 40)
                || invalid(details.addressLine1(), 200) || invalid(details.suburb(), 100)
                || details.state() == null || !List.of("VIC", "NSW", "QLD", "SA", "WA", "TAS", "ACT", "NT")
                    .contains(details.state().trim().toUpperCase(Locale.ROOT))
                || details.postcode() == null || !details.postcode().trim().matches("[0-9]{4}")
                || details.addressLine2() != null && details.addressLine2().length() > 200)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a complete Australian delivery address");
        String phone = australianPhone(details.phone());
        if (phone == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid Australian mobile or landline number");
        Long paid = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sales_orders WHERE build_request_id = ? AND status IN ('PAID', 'REWARD_ELIGIBLE')
                """, Long.class, requestId);
        if (paid != null && paid > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "A paid order's delivery address cannot be changed here");
        jdbc.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, recipient_name, phone, address_line_1, address_line_2, suburb, state, postcode)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (build_request_id) DO UPDATE SET
                    recipient_name = EXCLUDED.recipient_name, phone = EXCLUDED.phone,
                    address_line_1 = EXCLUDED.address_line_1, address_line_2 = EXCLUDED.address_line_2,
                    suburb = EXCLUDED.suburb, state = EXCLUDED.state, postcode = EXCLUDED.postcode
                """, requestId, details.recipientName().trim(), phone, details.addressLine1().trim(),
                blankToNull(details.addressLine2()), details.suburb().trim(), details.state().trim().toUpperCase(Locale.ROOT),
                details.postcode().trim());
        return find(requestId).orElseThrow();
    }

    private UUID ownedRequest(Authentication authentication, String reference) {
        UUID userId = UUID.fromString(authentication.getName());
        return jdbc.query("SELECT id FROM build_requests WHERE request_reference = ? AND user_id = ?",
                (rs, row) -> UUID.fromString(rs.getString(1)), reference, userId)
                .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order request not found"));
    }
    private boolean invalid(String value, int max) { return value == null || value.isBlank() || value.length() > max; }
    private String australianPhone(String value) {
        String compact = value.trim().replaceAll("[\\s()\\-]", "");
        if (compact.matches("0[42378][0-9]{8}")) return "+61" + compact.substring(1);
        if (compact.matches("\\+61[42378][0-9]{8}")) return compact;
        return null;
    }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record DeliveryDetails(String recipientName, String phone, String addressLine1, String addressLine2,
                                  String suburb, String state, String postcode) {}
}
