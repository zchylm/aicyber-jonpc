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
        ).invoice("Ezreal", "JON-INV-2026-000022", "SO-C7D67D8B3819", 254_900, 34_759L);

        assertTrue(email.subject().contains("Payment confirmed"));
        assertTrue(email.text().contains("Amount paid: $2,549.00"));
        assertTrue(email.html().contains("Order confirmation"));
        assertTrue(email.html().contains("SO-C7D67D8B3819"));
        assertTrue(email.html().contains("JON-INV-2026-000022 · PDF attached"));
        assertTrue(email.html().contains("Founder reward locked"));
        assertTrue(email.html().contains("support@jonpc.com.au"));
        assertFalse(email.html().contains("Separate from the amount paid"));
    }
}
