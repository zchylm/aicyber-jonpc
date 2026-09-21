package com.aicyber.backend.invoice.email;

import com.aicyber.backend.invoice.model.SalesInvoice;

public interface InvoiceEmailSender {
    String send(SalesInvoice invoice, java.util.UUID deliveryAttemptId);
}
