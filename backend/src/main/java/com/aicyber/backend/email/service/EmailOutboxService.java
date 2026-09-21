package com.aicyber.backend.email.service;

import com.aicyber.backend.email.model.EmailDraft;
import com.aicyber.backend.email.provider.EmailGateway;
import com.aicyber.backend.email.repository.EmailOutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
public class EmailOutboxService {
    private final EmailOutboxRepository repository;
    private final EmailGateway gateway;

    public EmailOutboxService(EmailOutboxRepository repository, EmailGateway gateway) {
        this.repository = repository;
        this.gateway = gateway;
    }

    @Transactional
    public UUID enqueue(EmailDraft draft) {
        validate(draft);
        return repository.enqueue(draft);
    }

    public String enqueueAndSend(EmailDraft draft) {
        UUID id = enqueue(draft);
        return deliver(id);
    }

    public String deliver(UUID id) {
        if (!repository.claim(id)) return repository.providerMessageId(id);
        var email = repository.findForDelivery(id).orElseThrow();
        try {
            String providerMessageId = gateway.send(email);
            repository.markAccepted(id, providerMessageId);
            return providerMessageId;
        } catch (RuntimeException exception) {
            if (email.attemptCount() >= 5) repository.markTerminalFailure(id, safeMessage(exception));
            else repository.markRetryPending(id, safeMessage(exception), nextAttempt(email.attemptCount()));
            throw exception;
        }
    }

    private OffsetDateTime nextAttempt(int attemptCount) {
        long minutes = switch (attemptCount) {
            case 1 -> 1;
            case 2 -> 5;
            case 3 -> 30;
            default -> 180;
        };
        return OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(minutes);
    }

    private void validate(EmailDraft draft) {
        if (draft == null || blank(draft.messageType()) || blank(draft.recipientEmail()) || blank(draft.subject())
                || blank(draft.idempotencyKey())) {
            throw new IllegalArgumentException("Email type, recipient, subject and idempotency key are required");
        }
        if (blank(draft.textBody()) && blank(draft.htmlBody())) {
            throw new IllegalArgumentException("Email must include text or HTML content");
        }
        if (draft.idempotencyKey().length() > 200) {
            throw new IllegalArgumentException("Email idempotency key is too long");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
