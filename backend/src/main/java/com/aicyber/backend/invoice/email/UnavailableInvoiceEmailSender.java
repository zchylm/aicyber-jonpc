package com.aicyber.backend.invoice.email;

import com.aicyber.backend.invoice.model.SalesInvoice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnExpression("'${jonpc.email.provider:disabled}' == 'disabled' || " +
        "('${jonpc.email.provider:disabled}' == 'resend' && !${jonpc.invoice.email-delivery-enabled:false})")
public class UnavailableInvoiceEmailSender implements InvoiceEmailSender {
    @Override
    public String send(SalesInvoice invoice, java.util.UUID deliveryAttemptId) {
        throw new IllegalStateException("Production invoice email delivery is not configured");
    }
}
