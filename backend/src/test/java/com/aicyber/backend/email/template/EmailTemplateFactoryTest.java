package com.aicyber.backend.email.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailTemplateFactoryTest {

    @Test
    void createsCompactBrandedInvoiceEmail() {
        EmailContent email = new EmailTemplateFactory(
                "https://jonpc.com.au/",
                "support@jonpc.com.au"
        ).invoice("Ezreal", "JON-INV-2026-000022", "SO-C7D67D8B3819",
                "JON PC Strike 5060", 254_900, 23, "Launch Founder", 34_759L);

        assertTrue(email.subject().contains("Your JON.PC is confirmed"));
        assertTrue(email.text().contains("Amount paid (GST included): $2,549.00"));
        assertTrue(email.html().contains("Order confirmation"));
        assertTrue(email.html().contains("JON PC Strike 5060"));
        assertTrue(email.html().contains("SO-C7D67D8B3819"));
        assertTrue(email.html().contains("Amount paid · GST included"));
        assertTrue(email.html().contains("JON-INV-2026-000022 · PDF attached"));
        assertTrue(email.html().contains("Founder 5% discount applied"));
        assertTrue(email.html().contains("You saved $347.59"));
        assertTrue(email.html().contains("We’ll keep you updated as your order progresses."));
        assertTrue(email.html().contains("support@jonpc.com.au"));
        assertFalse(email.html().contains("RTX 5060"));
        assertFalse(email.html().contains("Separate from the amount paid"));
    }

    @Test
    void createsConciseOrderLifecycleEmails() {
        EmailTemplateFactory templates = new EmailTemplateFactory(
                "https://jonpc.com.au/",
                "support@jonpc.com.au"
        );

        EmailContent dispatched = templates.orderDispatched("Ezreal", "SO-100", "Australia Post", "TRACK-100");
        EmailContent delivered = templates.deliveryConfirmationRequested(
                "Ezreal", "SO-100", false, java.time.OffsetDateTime.parse("2026-10-29T12:00:00+11:00"));

        assertTrue(dispatched.text().contains("TRACK-100"));
        assertTrue(dispatched.text().contains("open=orders&order=SO-100"));
        assertTrue(delivered.text().contains("confirm receipt or report a problem"));
        assertFalse(delivered.text().contains("Australian Consumer Law rights are unaffected"));
    }
}
