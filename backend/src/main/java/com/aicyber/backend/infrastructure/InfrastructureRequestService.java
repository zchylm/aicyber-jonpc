package com.aicyber.backend.infrastructure;

import com.aicyber.backend.auth.repository.UserRepository;
import com.aicyber.backend.email.service.CustomerEmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class InfrastructureRequestService {
    private static final List<String> REQUIRED_CONFIGURATION = List.of(
            "Scale", "Platform", "GPU", "CPU", "Memory", "Storage", "Network", "Systems",
            "System platform", "Power", "Cooling", "Chassis", "Operating system", "Management"
    );

    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final UserRepository users;
    private final CustomerEmailService customerEmails;

    public InfrastructureRequestService(JdbcTemplate jdbc, ObjectMapper json, UserRepository users,
                                        CustomerEmailService customerEmails) {
        this.jdbc = jdbc;
        this.json = json;
        this.users = users;
        this.customerEmails = customerEmails;
    }

    @Transactional
    public Response submit(UUID userId, Request request) {
        if (request == null || request.configuration() == null)
            throw new IllegalArgumentException("Infrastructure request must include a configuration");
        for (String field : REQUIRED_CONFIGURATION) {
            if (blank(request.configuration().get(field)))
                throw new IllegalArgumentException("Infrastructure configuration is missing " + field.toLowerCase(Locale.ROOT));
        }
        if (blank(request.organisation()) || blank(request.location()))
            throw new IllegalArgumentException("Organisation and project location are required");

        var user = users.findById(userId).orElseThrow(() -> new IllegalArgumentException("Account not found"));
        LinkedHashMap<String, String> answers = new LinkedHashMap<>();
        answers.put("Request type", "AI Infrastructure");
        answers.put("Organisation", limited(request.organisation(), 160));
        request.configuration().forEach((key, value) -> answers.put(limited(key, 80), limited(value, 500)));

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("direction", "AI Infrastructure");
        snapshot.put("answers", answers);
        for (String field : List.of("recommendedCpuId", "recommendedGpuId", "recommendedMemoryId",
                "recommendedStorageId", "recommendedMotherboardId", "recommendedPsuId", "recommendedCaseId",
                "recommendedCoolingId", "cpuId", "gpuId", "memoryId", "storageId", "motherboardId", "psuId",
                "caseId", "coolingId", "caseColorId")) snapshot.put(field, "");

        UUID requestId = UUID.randomUUID();
        String reference = "JON-AII-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
        jdbc.update("""
                INSERT INTO build_requests
                    (id, user_id, request_reference, name, email, phone, location, notes, contact_requested,
                     direction, estimated_price, recommended_baseline, selected_adjustments, configuration_snapshot)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, 'AI Infrastructure', 0, 0, 0, ?::jsonb)
                """, requestId, userId, reference, user.displayName(), user.email(), nullable(request.phone()),
                request.location().trim(), nullable(request.notes()), request.contact(), toJson(snapshot));
        customerEmails.buildReceived(requestId, user.email(), user.displayName(), reference);
        return new Response(reference, "RECEIVED",
                "Your infrastructure project has been submitted for engineering review.");
    }

    private String toJson(Object value) {
        try { return json.writeValueAsString(value); }
        catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Infrastructure configuration could not be saved", exception);
        }
    }

    private String nullable(String value) { return blank(value) ? null : limited(value, 2000); }
    private String limited(String value, int length) {
        if (value == null) return "";
        String trimmed = value.trim();
        return trimmed.length() <= length ? trimmed : trimmed.substring(0, length);
    }
    private boolean blank(String value) { return value == null || value.isBlank(); }

    public record Request(String organisation, String phone, String location, String notes, boolean contact,
                          Map<String, String> configuration) {}
    public record Response(String requestReference, String status, String message) {}
}
