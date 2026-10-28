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
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders/delivery")
public class DeliveryDetailsController {
    private static final Set<String> MELBOURNE_METRO_POSTCODES = Set.of(
            "3000", "3002", "3003", "3004", "3006", "3008", "3010", "3011", "3012", "3013", "3015", "3016", "3018", "3019",
            "3020", "3021", "3022", "3023", "3025", "3026", "3027", "3028", "3029", "3030", "3031", "3032", "3033", "3034", "3036", "3037", "3038", "3039",
            "3040", "3041", "3042", "3043", "3044", "3045", "3046", "3047", "3048", "3049", "3050", "3051", "3052", "3053", "3054", "3055", "3056", "3057", "3058", "3059",
            "3060", "3061", "3062", "3065", "3066", "3067", "3068", "3070", "3071", "3072", "3073", "3074", "3075", "3076", "3078", "3079", "3081", "3082", "3083", "3084", "3085", "3086", "3087", "3088", "3090", "3093", "3094", "3095",
            "3101", "3102", "3103", "3104", "3105", "3106", "3107", "3108", "3109", "3111", "3121", "3122", "3123", "3124", "3125", "3126", "3127", "3128", "3129", "3130", "3131", "3132", "3133", "3134", "3135", "3136", "3137", "3138", "3141", "3142", "3143", "3144", "3145", "3146", "3147", "3148", "3149", "3150", "3151", "3152", "3153", "3154", "3155", "3156", "3161", "3162", "3163", "3164", "3165", "3166", "3167", "3168", "3169", "3170", "3171", "3172", "3173", "3174", "3175", "3177", "3178", "3179", "3180", "3181", "3182", "3183", "3184", "3185", "3186", "3187", "3188", "3189", "3190", "3191", "3192", "3193", "3194", "3195", "3196", "3197", "3198", "3199",
            "3200", "3201", "3202", "3204", "3205", "3206", "3207", "3752", "3802", "3803", "3805", "3975", "3976"
    );
    private static final String PICKUP_ADDRESS = "205 Kensington Road";
    private static final String PICKUP_SUBURB = "West Melbourne";
    private static final String PICKUP_POSTCODE = "3003";
    private final JdbcTemplate jdbc;
    public DeliveryDetailsController(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @GetMapping("/{reference}")
    public ResponseEntity<DeliveryDetails> get(Authentication authentication, @PathVariable String reference) {
        UUID requestId = ownedRequest(authentication, reference);
        return find(requestId).map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    private java.util.Optional<DeliveryDetails> find(UUID requestId) {
        return jdbc.query("""
                SELECT delivery_method, recipient_name, phone, address_line_1, address_line_2, suburb, state, postcode
                FROM build_delivery_details WHERE build_request_id = ?
                """, (rs, row) -> new DeliveryDetails(rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4),
                rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8)), requestId)
                .stream().findFirst();
    }

    @PutMapping("/{reference}")
    @Transactional
    public DeliveryDetails save(Authentication authentication, @PathVariable String reference, @RequestBody DeliveryDetails details) {
        UUID requestId = ownedRequest(authentication, reference);
        String method = details == null || details.deliveryMethod() == null ? "DELIVERY" : details.deliveryMethod().trim().toUpperCase(Locale.ROOT);
        if (details == null || !List.of("DELIVERY", "PICKUP").contains(method)
                || invalid(details.recipientName(), 160) || invalid(details.phone(), 40)
                || "DELIVERY".equals(method) && (invalid(details.addressLine1(), 200) || invalid(details.suburb(), 100)
                || details.state() == null || !List.of("VIC", "NSW", "QLD", "SA", "WA", "TAS", "ACT", "NT")
                    .contains(details.state().trim().toUpperCase(Locale.ROOT))
                || details.postcode() == null || !details.postcode().trim().matches("[0-9]{4}")
                || details.addressLine2() != null && details.addressLine2().length() > 200))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a complete Australian delivery address");
        if ("DELIVERY".equals(method) && (!"VIC".equalsIgnoreCase(details.state())
                || !MELBOURNE_METRO_POSTCODES.contains(details.postcode().trim()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A$25 delivery is available to Melbourne metro postcodes. Contact support for a delivery quote.");
        }
        String phone = australianPhone(details.phone());
        if (phone == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a valid Australian mobile or landline number");
        Long paid = jdbc.queryForObject("""
                SELECT COUNT(*) FROM sales_orders WHERE build_request_id = ? AND status = 'PAID'
                """, Long.class, requestId);
        if (paid != null && paid > 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "A paid order's delivery address cannot be changed here");
        jdbc.update("""
                INSERT INTO build_delivery_details
                    (build_request_id, delivery_method, recipient_name, phone, address_line_1, address_line_2, suburb, state, postcode)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (build_request_id) DO UPDATE SET
                    delivery_method = EXCLUDED.delivery_method, recipient_name = EXCLUDED.recipient_name, phone = EXCLUDED.phone,
                    address_line_1 = EXCLUDED.address_line_1, address_line_2 = EXCLUDED.address_line_2,
                    suburb = EXCLUDED.suburb, state = EXCLUDED.state, postcode = EXCLUDED.postcode
                """, requestId, method, details.recipientName().trim(), phone,
                "PICKUP".equals(method) ? PICKUP_ADDRESS : details.addressLine1().trim(),
                "PICKUP".equals(method) ? null : blankToNull(details.addressLine2()),
                "PICKUP".equals(method) ? PICKUP_SUBURB : details.suburb().trim(),
                "PICKUP".equals(method) ? "VIC" : details.state().trim().toUpperCase(Locale.ROOT),
                "PICKUP".equals(method) ? PICKUP_POSTCODE : details.postcode().trim());
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

    public record DeliveryDetails(String deliveryMethod, String recipientName, String phone, String addressLine1, String addressLine2,
                                  String suburb, String state, String postcode) {
        public DeliveryDetails(String recipientName, String phone, String addressLine1, String addressLine2,
                               String suburb, String state, String postcode) {
            this("DELIVERY", recipientName, phone, addressLine1, addressLine2, suburb, state, postcode);
        }
    }
}
