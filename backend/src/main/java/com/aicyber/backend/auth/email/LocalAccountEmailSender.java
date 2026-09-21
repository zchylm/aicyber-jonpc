package com.aicyber.backend.auth.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jonpc.email.provider", havingValue = "local")
public class LocalAccountEmailSender implements AccountEmailSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalAccountEmailSender.class);

    @Override
    public void sendVerification(String email, String displayName, String verificationUrl) {
        LOGGER.info("LOCAL EMAIL PREVIEW for {} ({}): {}", displayName, email, verificationUrl);
    }

    @Override
    public void sendPasswordReset(String email, String displayName, String resetUrl) {
        LOGGER.info("LOCAL PASSWORD RESET PREVIEW for {} ({}): {}", displayName, email, resetUrl);
    }
}
