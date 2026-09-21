package com.aicyber.backend.email.service;

import com.aicyber.backend.configurator.dto.ConfiguratorBuildRequest;
import com.aicyber.backend.email.model.EmailDraft;
import com.aicyber.backend.email.template.EmailContent;
import com.aicyber.backend.email.template.EmailTemplateFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class CustomerEmailService {
    private final EmailOutboxService outbox;
    private final EmailTemplateFactory templates;
    private final String operationsRecipient;

    public CustomerEmailService(
            EmailOutboxService outbox,
            EmailTemplateFactory templates,
            @Value("${jonpc.email.operations-recipient:}") String operationsRecipient
    ) {
        this.outbox = outbox;
        this.templates = templates;
        this.operationsRecipient = operationsRecipient;
    }

    public void buildReceived(UUID requestId, ConfiguratorBuildRequest request, String reference) {
        EmailContent customer = templates.buildReceived(request.name(), reference);
        outbox.enqueue(draft("CUSTOM_BUILD_RECEIVED", request.email(), request.name(), customer,
                "BUILD_REQUEST", requestId, "custom-build-received:" + requestId));

        if (operationsRecipient != null && !operationsRecipient.isBlank()) {
            EmailContent operations = templates.operationsBuildReceived(reference, request.name());
            outbox.enqueue(draft("OPERATIONS_BUILD_ALERT", operationsRecipient, "JON. PC Operations", operations,
                    "BUILD_REQUEST", requestId, "operations-build-alert:" + requestId));
        }
    }

    public void quoteReady(UUID requestId, UUID quoteId, String email, String name, String reference,
                           long totalCents, OffsetDateTime validUntil) {
        EmailContent content = templates.quoteReady(name, reference, totalCents, validUntil);
        outbox.enqueue(draft("CUSTOM_QUOTE_READY", email, name, content,
                "CUSTOM_BUILD_QUOTE", quoteId, "custom-quote-ready:" + quoteId));
    }

    private EmailDraft draft(String type, String recipient, String name, EmailContent content,
                             String aggregateType, UUID aggregateId, String idempotencyKey) {
        return new EmailDraft(type, recipient, name, content.subject(), content.text(), content.html(), null,
                aggregateType, aggregateId, idempotencyKey);
    }
}

