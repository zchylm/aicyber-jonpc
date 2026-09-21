package com.aicyber.backend.invoice.service;

import com.aicyber.backend.invoice.email.InvoiceEmailSender;
import com.aicyber.backend.invoice.model.InvoiceDeliveryAttempt;
import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InvoiceDeliveryService {
    private final InvoiceRepository repository;
    private final InvoiceEmailSender emailSender;

    public InvoiceDeliveryService(InvoiceRepository repository, InvoiceEmailSender emailSender) {
        this.repository = repository;
        this.emailSender = emailSender;
    }

    public void deliver(UUID invoiceId) {
        InvoiceDeliveryAttempt attempt = repository.deliveryAttempt(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice delivery record was not found"));
        if ("SENT".equals(attempt.status())) return;

        SalesInvoice invoice = repository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice was not found"));
        send(invoice, attempt);
    }

    public void redeliver(UUID invoiceId) {
        SalesInvoice invoice = repository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice was not found"));
        InvoiceDeliveryAttempt attempt = repository.createDeliveryAttempt(invoice.id(), invoice.buyerEmail());
        send(invoice, attempt);
    }

    private void send(SalesInvoice invoice, InvoiceDeliveryAttempt attempt) {
        try {
            repository.markDeliverySent(attempt.id(), emailSender.send(invoice, attempt.id()));
        } catch (RuntimeException exception) {
            repository.markDeliveryFailed(attempt.id(), safeMessage(exception));
            throw exception;
        }
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        if (message == null || message.isBlank()) return exception.getClass().getSimpleName();
        return message.length() <= 500 ? message : message.substring(0, 500);
    }
}
