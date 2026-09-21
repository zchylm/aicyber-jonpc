package com.aicyber.backend.admin.service;

import com.aicyber.backend.email.service.CustomerEmailService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CustomBuildReviewService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final CustomerEmailService customerEmails;

    public CustomBuildReviewService(JdbcTemplate jdbc, ObjectMapper json, CustomerEmailService customerEmails) {
        this.jdbc = jdbc;
        this.json = json;
        this.customerEmails = customerEmails;
    }

    public List<ReviewItem> list() {
        return jdbc.query("""
                SELECT b.id, b.request_reference, b.name, b.email, b.phone, b.notes, b.direction, b.estimated_price,
                       b.configuration_snapshot::text, b.created_at, q.id AS quote_id,
                       q.version AS quote_version, q.status AS quote_status,
                       q.total_cents, q.review_note, q.valid_until
                FROM build_requests b
                LEFT JOIN LATERAL (
                    SELECT id, version, status, total_cents, review_note, valid_until
                    FROM custom_build_quotes WHERE build_request_id = b.id
                    ORDER BY version DESC LIMIT 1
                ) q ON TRUE
                WHERE b.user_id IS NOT NULL
                  AND b.configuration_snapshot->'answers'->>'systemSku' IS NULL
                  AND b.status <> 'CANCELLED'
                  AND (q.status = 'ACCEPTED' OR NOT EXISTS
                      (SELECT 1 FROM sales_orders s WHERE s.build_request_id = b.id))
                ORDER BY b.created_at DESC LIMIT 100
                """, (rs, row) -> new ReviewItem(
                UUID.fromString(rs.getString("id")), rs.getString("request_reference"),
                rs.getString("name"), rs.getString("email"), rs.getString("phone"), rs.getString("notes"), rs.getString("direction"),
                rs.getInt("estimated_price"), readConfiguration(rs.getString("configuration_snapshot")),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getString("quote_id") == null ? null : UUID.fromString(rs.getString("quote_id")),
                rs.getInt("quote_version"), rs.getString("quote_status"), rs.getLong("total_cents"),
                rs.getString("review_note"), rs.getObject("valid_until", OffsetDateTime.class)
        ));
    }

    @Transactional
    public ReviewItem publish(UUID requestId, UUID reviewerId, PublishQuote request) {
        if (request == null || request.totalCents() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a positive final AUD amount");
        if (request.validUntil() != null && !request.validUntil().isAfter(OffsetDateTime.now()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quote expiry must be in the future");
        int locked = jdbc.query("""
                SELECT 1 FROM build_requests
                WHERE id = ? AND user_id IS NOT NULL AND status <> 'CANCELLED'
                  AND configuration_snapshot->'answers'->>'systemSku' IS NULL
                FOR UPDATE
                """, rs -> rs.next() ? 1 : 0, requestId);
        if (locked == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Custom build request not found");
        List<String> statuses = jdbc.query("""
                SELECT status FROM custom_build_quotes WHERE build_request_id = ? ORDER BY version DESC LIMIT 1
                """, (rs, row) -> rs.getString(1), requestId);
        if (!statuses.isEmpty() && "ACCEPTED".equals(statuses.get(0)))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An accepted quote cannot be changed");
        Long existingOrder = jdbc.queryForObject("SELECT COUNT(*) FROM sales_orders WHERE build_request_id = ?", Long.class, requestId);
        if (existingOrder != null && existingOrder > 0)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An existing order cannot be reviewed again");
        jdbc.update("UPDATE custom_build_quotes SET status = 'SUPERSEDED' WHERE build_request_id = ? AND status = 'SENT'", requestId);
        Integer nextVersion = jdbc.queryForObject("SELECT COALESCE(MAX(version), 0) + 1 FROM custom_build_quotes WHERE build_request_id = ?", Integer.class, requestId);
        UUID quoteId = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO custom_build_quotes
                    (id, build_request_id, version, status, total_cents, reviewer_id, review_note, valid_until)
                VALUES (?, ?, ?, 'SENT', ?, ?, ?, ?)
                """, quoteId, requestId, nextVersion, request.totalCents(), reviewerId,
                blankToNull(request.reviewNote()), request.validUntil());
        ReviewItem published = list().stream().filter(item -> item.id().equals(requestId)).findFirst().orElseThrow();
        customerEmails.quoteReady(requestId, quoteId, published.email(), published.name(),
                published.requestReference(), request.totalCents(), request.validUntil());
        return published;
    }

    @Transactional
    public void accept(UUID quoteId, UUID userId) {
        List<QuoteOwner> matches = jdbc.query("""
                SELECT q.build_request_id, q.status, q.valid_until, b.user_id
                FROM custom_build_quotes q JOIN build_requests b ON b.id = q.build_request_id
                WHERE q.id = ? FOR UPDATE OF q
                """, (rs, row) -> new QuoteOwner(UUID.fromString(rs.getString("build_request_id")),
                rs.getString("status"), rs.getObject("valid_until", OffsetDateTime.class),
                UUID.fromString(rs.getString("user_id"))), quoteId);
        if (matches.isEmpty() || !matches.get(0).userId().equals(userId))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Quote not found for this account");
        QuoteOwner quote = matches.get(0);
        if ("ACCEPTED".equals(quote.status())) return;
        if (!"SENT".equals(quote.status()) || quote.validUntil() != null && quote.validUntil().isBefore(OffsetDateTime.now()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Quote is no longer available");
        jdbc.update("UPDATE custom_build_quotes SET status = 'ACCEPTED', accepted_at = CURRENT_TIMESTAMP WHERE id = ?", quoteId);
    }

    private Map<String, Object> readConfiguration(String value) {
        try { return json.readValue(value, new TypeReference<>() {}); }
        catch (JsonProcessingException exception) { throw new IllegalStateException("Saved configuration is invalid", exception); }
    }

    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record PublishQuote(long totalCents, String reviewNote, OffsetDateTime validUntil) {}
    public record ReviewItem(UUID id, String requestReference, String name, String email, String phone, String notes, String direction,
                             int estimatedPrice, Map<String, Object> configuration, OffsetDateTime createdAt,
                             UUID quoteId, int quoteVersion, String quoteStatus, long totalCents,
                             String reviewNote, OffsetDateTime validUntil) {}
    private record QuoteOwner(UUID requestId, String status, OffsetDateTime validUntil, UUID userId) {}
}
