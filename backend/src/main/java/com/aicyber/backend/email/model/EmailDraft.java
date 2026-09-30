package com.aicyber.backend.email.model;

import java.util.UUID;

public record EmailDraft(
        String messageType,
        String recipientEmail,
        String recipientName,
        String sender,
        String subject,
        String textBody,
        String htmlBody,
        EmailAttachment attachment,
        String aggregateType,
        UUID aggregateId,
        String idempotencyKey
) {
}
