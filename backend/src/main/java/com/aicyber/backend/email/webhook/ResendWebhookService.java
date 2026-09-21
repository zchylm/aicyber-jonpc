package com.aicyber.backend.email.webhook;

import com.aicyber.backend.email.repository.EmailOutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
public class ResendWebhookService {
    private final ObjectMapper json;
    private final EmailOutboxRepository repository;

    public ResendWebhookService(ObjectMapper json, EmailOutboxRepository repository) {
        this.json = json;
        this.repository = repository;
    }

    @Transactional
    public void handle(String eventId, byte[] payload) {
        try {
            JsonNode event = json.readTree(payload);
            String type = event.path("type").asText();
            String providerMessageId = event.path("data").path("email_id").asText();
            if (type.isBlank()) throw new IllegalArgumentException("Resend event type is missing");
            if (!repository.recordWebhookEvent(eventId, type, providerMessageId.isBlank() ? null : providerMessageId)) return;
            String status = status(type);
            if (status != null && !providerMessageId.isBlank()) repository.applyProviderEvent(providerMessageId, status);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Resend webhook payload is invalid", exception);
        }
    }

    private String status(String type) {
        return switch (type) {
            case "email.sent" -> "ACCEPTED";
            case "email.delivered" -> "DELIVERED";
            case "email.delivery_delayed" -> "DELAYED";
            case "email.failed" -> "FAILED";
            case "email.bounced" -> "BOUNCED";
            case "email.complained" -> "COMPLAINED";
            case "email.suppressed" -> "SUPPRESSED";
            default -> null;
        };
    }
}

