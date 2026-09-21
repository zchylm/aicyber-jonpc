package com.aicyber.backend.email.service;

import com.aicyber.backend.email.repository.EmailOutboxRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "jonpc.email.dispatch-enabled", havingValue = "true")
public class EmailOutboxDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailOutboxDispatcher.class);
    private final EmailOutboxRepository repository;
    private final EmailOutboxService service;

    public EmailOutboxDispatcher(EmailOutboxRepository repository, EmailOutboxService service) {
        this.repository = repository;
        this.service = service;
    }

    @Scheduled(fixedDelayString = "${jonpc.email.dispatch-interval-ms:5000}")
    public void dispatch() {
        for (var id : repository.readyIds(20)) {
            try {
                service.deliver(id);
            } catch (RuntimeException exception) {
                LOGGER.warn("Email {} delivery failed: {}", id, exception.getMessage());
            }
        }
    }
}

