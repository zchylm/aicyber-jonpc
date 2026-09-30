package com.aicyber.backend.email.repository;

import com.aicyber.backend.email.model.EmailAttachment;
import com.aicyber.backend.email.model.EmailDraft;
import com.aicyber.backend.email.model.QueuedEmail;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class EmailOutboxRepository {
    private final JdbcTemplate jdbc;

    public EmailOutboxRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID enqueue(EmailDraft draft) {
        UUID id = UUID.randomUUID();
        EmailAttachment attachment = draft.attachment();
        jdbc.update("""
                INSERT INTO transactional_email_outbox
                    (id, message_type, recipient_email, recipient_name, sender, subject, text_body, html_body,
                     attachment_filename, attachment_content_type, attachment_content,
                     aggregate_type, aggregate_id, idempotency_key)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (idempotency_key) DO NOTHING
                """, id, draft.messageType(), draft.recipientEmail(), draft.recipientName(), draft.sender(), draft.subject(),
                draft.textBody(), draft.htmlBody(), attachment == null ? null : attachment.filename(),
                attachment == null ? null : attachment.contentType(), attachment == null ? null : attachment.content(),
                draft.aggregateType(), draft.aggregateId(), draft.idempotencyKey());
        return jdbc.queryForObject(
                "SELECT id FROM transactional_email_outbox WHERE idempotency_key = ?",
                (rs, row) -> UUID.fromString(rs.getString(1)), draft.idempotencyKey()
        );
    }

    public List<UUID> readyIds(int limit) {
        return jdbc.query("""
                SELECT id FROM transactional_email_outbox
                WHERE (status IN ('QUEUED', 'RETRY_PENDING') AND next_attempt_at <= CURRENT_TIMESTAMP
                       OR status = 'SENDING' AND last_attempted_at < CURRENT_TIMESTAMP - INTERVAL '10 minutes')
                  AND attempt_count < 5
                ORDER BY next_attempt_at, queued_at
                LIMIT ?
                """, (rs, row) -> UUID.fromString(rs.getString(1)), limit);
    }

    public boolean claim(UUID id) {
        return jdbc.update("""
                UPDATE transactional_email_outbox
                SET status = 'SENDING', attempt_count = attempt_count + 1,
                    last_attempted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP, last_error = NULL
                WHERE id = ? AND attempt_count < 5
                  AND (status IN ('QUEUED', 'RETRY_PENDING') AND next_attempt_at <= CURRENT_TIMESTAMP
                       OR status = 'SENDING' AND last_attempted_at < CURRENT_TIMESTAMP - INTERVAL '10 minutes')
                """, id) == 1;
    }

    public Optional<QueuedEmail> findForDelivery(UUID id) {
        return jdbc.query("""
                SELECT id, message_type, recipient_email, recipient_name, sender, subject, text_body, html_body,
                       attachment_filename, attachment_content_type, attachment_content,
                       idempotency_key, attempt_count
                FROM transactional_email_outbox WHERE id = ? AND status = 'SENDING'
                """, rs -> rs.next() ? Optional.of(map(rs)) : Optional.empty(), id);
    }

    public void markAccepted(UUID id, String providerMessageId) {
        jdbc.update("""
                UPDATE transactional_email_outbox
                SET status = 'ACCEPTED', provider_message_id = ?, accepted_at = CURRENT_TIMESTAMP,
                    text_body = NULL, html_body = NULL,
                    attachment_filename = NULL, attachment_content_type = NULL, attachment_content = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, providerMessageId, id);
    }

    public void markRetryPending(UUID id, String error, OffsetDateTime nextAttemptAt) {
        jdbc.update("""
                UPDATE transactional_email_outbox
                SET status = 'RETRY_PENDING', last_error = ?, next_attempt_at = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, error, nextAttemptAt, id);
    }

    public void markTerminalFailure(UUID id, String error) {
        jdbc.update("""
                UPDATE transactional_email_outbox
                SET status = 'FAILED', last_error = ?,
                    text_body = NULL, html_body = NULL,
                    attachment_filename = NULL, attachment_content_type = NULL, attachment_content = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, error, id);
    }

    public String providerMessageId(UUID id) {
        return jdbc.query("SELECT provider_message_id FROM transactional_email_outbox WHERE id = ?",
                rs -> rs.next() ? rs.getString(1) : null, id);
    }

    public boolean recordWebhookEvent(String eventId, String eventType, String providerMessageId) {
        return jdbc.update("""
                INSERT INTO transactional_email_webhook_events (event_id, event_type, provider_message_id)
                VALUES (?, ?, ?) ON CONFLICT (event_id) DO NOTHING
                """, eventId, eventType, providerMessageId) == 1;
    }

    public void applyProviderEvent(String providerMessageId, String status) {
        String deliveredAt = "DELIVERED".equals(status) ? ", delivered_at = CURRENT_TIMESTAMP" : "";
        String allowedCurrentStatuses = switch (status) {
            case "ACCEPTED" -> "('ACCEPTED')";
            case "DELIVERED" -> "('ACCEPTED', 'DELAYED')";
            case "DELAYED" -> "('ACCEPTED')";
            case "COMPLAINED" -> "('ACCEPTED', 'DELIVERED')";
            default -> "('ACCEPTED', 'DELAYED')";
        };
        jdbc.update("""
                UPDATE transactional_email_outbox SET status = ?, updated_at = CURRENT_TIMESTAMP
                """ + deliveredAt + " WHERE provider_message_id = ? AND status IN " + allowedCurrentStatuses,
                status, providerMessageId);
    }

    private QueuedEmail map(ResultSet rs) throws SQLException {
        byte[] content = rs.getBytes("attachment_content");
        EmailAttachment attachment = content == null ? null : new EmailAttachment(
                rs.getString("attachment_filename"), rs.getString("attachment_content_type"), content);
        return new QueuedEmail(
                UUID.fromString(rs.getString("id")), rs.getString("message_type"),
                rs.getString("recipient_email"), rs.getString("recipient_name"), rs.getString("sender"), rs.getString("subject"),
                rs.getString("text_body"), rs.getString("html_body"), attachment,
                rs.getString("idempotency_key"), rs.getInt("attempt_count")
        );
    }
}
