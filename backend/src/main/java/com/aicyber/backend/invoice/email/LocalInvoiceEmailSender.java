package com.aicyber.backend.invoice.email;

import com.aicyber.backend.invoice.model.SalesInvoice;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(name = "jonpc.email.provider", havingValue = "local")
public class LocalInvoiceEmailSender implements InvoiceEmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalInvoiceEmailSender.class);
    private final String previewBaseUrl;

    public LocalInvoiceEmailSender(
            @Value("${jonpc.invoice.local-preview-base-url:http://localhost:8080}") String previewBaseUrl
    ) {
        this.previewBaseUrl = previewBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public String send(SalesInvoice invoice, UUID deliveryAttemptId) {
        String messageId = "LOCAL-" + UUID.randomUUID();
        LOGGER.info(
                "LOCAL TAX INVOICE PREVIEW {} for {} ({} bytes). Download from {}/api/local/invoices/{}/pdf",
                invoice.invoiceNumber(), invoice.buyerEmail(), invoice.pdfContent().length,
                previewBaseUrl, invoice.id()
        );
        return messageId;
    }
}
