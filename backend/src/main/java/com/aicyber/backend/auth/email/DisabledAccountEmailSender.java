package com.aicyber.backend.auth.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jonpc.email.provider", havingValue = "disabled", matchIfMissing = true)
public class DisabledAccountEmailSender implements AccountEmailSender {
    @Override
    public void sendVerification(String email, String displayName, String verificationUrl) {
        throw new IllegalStateException("Transactional email delivery is not configured");
    }

    @Override
    public void sendPasswordReset(String email, String displayName, String resetUrl) {
        throw new IllegalStateException("Transactional email delivery is not configured");
    }
}

