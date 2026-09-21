package com.aicyber.backend.catalog;

import com.aicyber.backend.auth.model.User;
import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/systems/local-requests")
@Profile("local")
@ConditionalOnProperty(name = "jonpc.payments.demo-enabled", havingValue = "true")
public class LocalSystemRequestController {
    private final SystemCatalogService catalog;
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;

    public LocalSystemRequestController(SystemCatalogService catalog, UserRepository users, JdbcTemplate jdbc, ObjectMapper json) {
        this.catalog = catalog;
        this.users = users;
        this.jdbc = jdbc;
        this.json = json;
    }

    @PostMapping
    @Transactional
    public LocalSystemRequestResponse create(Authentication authentication, @RequestBody LocalSystemRequest request) {
        if (request == null || request.sku() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Select a system");
        UUID userId = UUID.fromString(authentication.getName());
        User user = users.findById(userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        SystemProduct system = catalog.previews().stream().filter(item -> item.sku().equals(request.sku())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "System preview not found"));
        if (system.priceCents() != request.expectedPriceCents())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "System price changed. Refresh before checkout");
        if (system.availableQuantity() <= 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "System is sold out");
        if (system.priceCents() <= 0 || system.priceCents() % 100 != 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This system cannot use the current local checkout amount");
        String reference = "JON-SYS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase(Locale.ROOT);
        Map<String, String> answers = new HashMap<>(system.specifications());
        answers.put("systemSku", system.sku());
        answers.put("systemId", system.id().toString());
        answers.put("systemName", system.name());
        ConfiguratorQuoteRequest snapshot = new ConfiguratorQuoteRequest("gaming", answers,
                null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);
        try {
            jdbc.update("""
                    INSERT INTO build_requests
                        (id, user_id, request_reference, name, email, location, notes, contact_requested,
                         direction, estimated_price, recommended_baseline, selected_adjustments, configuration_snapshot)
                    VALUES (?, ?, ?, ?, ?, 'Online', 'Local system checkout test', FALSE,
                            'gaming', ?, ?, 0, ?::jsonb)
                    """, UUID.randomUUID(), userId, reference, user.displayName(), user.email(),
                    Math.toIntExact(system.priceCents() / 100), Math.toIntExact(system.priceCents() / 100),
                    json.writeValueAsString(snapshot));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("System snapshot could not be saved", exception);
        }
        return new LocalSystemRequestResponse(reference, system.sku(), system.name(), system.priceCents());
    }

    public record LocalSystemRequest(String sku, long expectedPriceCents) {}
    public record LocalSystemRequestResponse(String requestReference, String sku, String name, long priceCents) {}
}
