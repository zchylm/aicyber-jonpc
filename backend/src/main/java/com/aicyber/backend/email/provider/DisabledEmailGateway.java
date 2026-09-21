package com.aicyber.backend.email.provider;

import com.aicyber.backend.email.model.QueuedEmail;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jonpc.email.provider", havingValue = "disabled", matchIfMissing = true)
public class DisabledEmailGateway implements EmailGateway {
    @Override
    public String send(QueuedEmail email) {
        throw new IllegalStateException("Transactional email delivery is not configured");
    }
}
