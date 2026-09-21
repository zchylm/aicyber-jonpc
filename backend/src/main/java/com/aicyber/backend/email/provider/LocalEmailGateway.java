package com.aicyber.backend.email.provider;

import com.aicyber.backend.email.model.QueuedEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jonpc.email.provider", havingValue = "local")
public class LocalEmailGateway implements EmailGateway {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalEmailGateway.class);

    @Override
    public String send(QueuedEmail email) {
        LOGGER.info("LOCAL EMAIL {} to {}: {}", email.messageType(), email.recipientEmail(), email.textBody());
        return "LOCAL-" + email.id();
    }
}
