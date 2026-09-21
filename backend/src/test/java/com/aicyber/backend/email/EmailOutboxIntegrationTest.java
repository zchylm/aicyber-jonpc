package com.aicyber.backend.email;

import com.aicyber.backend.email.model.EmailDraft;
import com.aicyber.backend.email.service.EmailOutboxService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringBootTest(properties = "jonpc.email.dispatch-enabled=false")
@ActiveProfiles("local")
@Transactional
class EmailOutboxIntegrationTest {
    @Autowired EmailOutboxService outbox;
    @Autowired JdbcTemplate jdbc;

    @Test
    void storesSendsAndScrubsSensitiveContent() {
        var id = outbox.enqueue(new EmailDraft("ACCOUNT_VERIFICATION", "test@example.com", "Test",
                "Verify", "secret link", "<p>secret link</p>", null,
                "ACCOUNT", null, "verify:test-key"));

        outbox.deliver(id);

        assertEquals("ACCEPTED", jdbc.queryForObject(
                "SELECT status FROM transactional_email_outbox WHERE id = ?", String.class, id));
        assertNull(jdbc.queryForObject(
                "SELECT text_body FROM transactional_email_outbox WHERE id = ?", String.class, id));
    }
}

