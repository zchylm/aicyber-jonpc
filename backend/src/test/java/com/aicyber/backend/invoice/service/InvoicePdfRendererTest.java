package com.aicyber.backend.invoice.service;

import com.aicyber.backend.invoice.model.InvoiceLine;
import com.aicyber.backend.invoice.model.SalesInvoice;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvoicePdfRendererTest {
    private final InvoicePdfRenderer renderer = new InvoicePdfRenderer();

    @Test
    void rendersAReadableOnePageAustralianTaxInvoice() throws Exception {
        SalesInvoice invoice = sampleInvoice();
        byte[] pdf = renderer.render(invoice);

        Path output = Path.of("target", "test-output", "jon-pc-tax-invoice-sample.pdf");
        Files.createDirectories(output.getParent());
        Files.write(output, pdf);

        try (PDDocument document = Loader.loadPDF(pdf)) {
            assertEquals(1, document.getNumberOfPages());
            String text = new PDFTextStripper().getText(document);
            assertTrue(text.contains("TAX INVOICE"));
            assertTrue(text.contains("AI CYBER AUSTRALIA PTY LTD"));
            assertTrue(text.contains("ABN 22 689 546 450"));
            assertTrue(text.contains("JON-INV-2026-000001"));
            assertTrue(text.contains("Customer ID  JON-CUS-7A3F2C91D8E4"));
            assertTrue(text.contains("JON. PC Gaming custom computer system"));
            assertTrue(text.contains("Subtotal excluding GST"));
            assertTrue(text.contains("$213.36"));
            assertTrue(text.contains("Total including GST"));
            assertTrue(text.contains("$2,347.00"));
            assertTrue(text.contains("Total price includes GST"));
            assertTrue(text.contains("FOUNDER CASHBACK COMMITMENT"));
            assertTrue(text.contains("Founder #08"));
            assertTrue(text.contains("$381.68"));
        }
    }

    private SalesInvoice sampleInvoice() {
        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), 1, null, "JON. PC Gaming custom computer system", 1,
                213_364, 1000, 21_336, 234_700, true
        );
        return new SalesInvoice(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "JON-INV-2026-000001", "TAX_INVOICE", "ISSUED", "SO-EXAMPLE0001", "PAY-EXAMPLE001",
                "AI CYBER AUSTRALIA PTY LTD", "JON. PC", "22 689 546 450",
                "205 Kensington Rd, West Melbourne VIC 3003, Australia",
                "sales@aicybermedia.com.au", "+61 436 365 016", "Jambo Customer",
                "customer@example.com", "JON-CUS-7A3F2C91D8E4", "Melbourne", null, "AUD", 213_364, 21_336,
                234_700, 234_700, OffsetDateTime.parse("2026-09-15T10:30:00+10:00"),
                8, "Launch Founder", 38_168L,
                "Separate from the amount paid. JON.PC initiates eligible cashback within 30 days after confirmed delivery.",
                List.of(line), null, null
        );
    }
}
