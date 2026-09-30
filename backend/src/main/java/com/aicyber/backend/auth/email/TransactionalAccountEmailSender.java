package com.aicyber.backend.auth.email;

import com.aicyber.backend.email.model.EmailDraft;
import com.aicyber.backend.email.service.EmailOutboxService;
import com.aicyber.backend.email.template.EmailContent;
import com.aicyber.backend.email.template.EmailTemplateFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@ConditionalOnProperty(name = "jonpc.email.provider", havingValue = "resend")
public class TransactionalAccountEmailSender implements AccountEmailSender {
    private final EmailOutboxService outbox;
    private final EmailTemplateFactory templates;

    public TransactionalAccountEmailSender(EmailOutboxService outbox, EmailTemplateFactory templates) {
        this.outbox = outbox;
        this.templates = templates;
    }

    @Override
    public void sendVerification(String email, String displayName, String verificationUrl) {
        EmailContent content = templates.verification(displayName, verificationUrl);
        outbox.enqueue(draft("ACCOUNT_VERIFICATION", email, displayName, content,
                "account-verification:" + hash(verificationUrl)));
    }

    @Override
    public void sendPasswordReset(String email, String displayName, String resetUrl) {
        EmailContent content = templates.passwordReset(displayName, resetUrl);
        outbox.enqueue(draft("PASSWORD_RESET", email, displayName, content,
                "password-reset:" + hash(resetUrl)));
    }

    private EmailDraft draft(String type, String email, String displayName, EmailContent content, String key) {
        return new EmailDraft(type, email, displayName, null, content.subject(), content.text(), content.html(), null,
                "ACCOUNT", null, key);
    }

    private String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
