package com.aicyber.backend.invoice.email;

import com.aicyber.backend.email.model.EmailAttachment;
import com.aicyber.backend.email.model.EmailDraft;
import com.aicyber.backend.email.service.EmailOutboxService;
import com.aicyber.backend.email.template.EmailContent;
import com.aicyber.backend.email.template.EmailTemplateFactory;
import com.aicyber.backend.invoice.model.SalesInvoice;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnExpression("'${jonpc.email.provider:disabled}' == 'resend' && ${jonpc.invoice.email-delivery-enabled:false}")
public class TransactionalInvoiceEmailSender implements InvoiceEmailSender {
    private final EmailOutboxService outbox;
    private final EmailTemplateFactory templates;

    public TransactionalInvoiceEmailSender(EmailOutboxService outbox, EmailTemplateFactory templates) {
        this.outbox = outbox;
        this.templates = templates;
    }

    @Override
    public String send(SalesInvoice invoice, UUID deliveryAttemptId) {
        EmailContent content = templates.invoice(invoice.buyerName(), invoice.invoiceNumber(), invoice.orderReference(),
                invoice.founderCashbackAmountCents(), invoice.founderCashbackTerms());
        return outbox.enqueueAndSend(new EmailDraft(
                "TAX_INVOICE", invoice.buyerEmail(), invoice.buyerName(), content.subject(), content.text(), content.html(),
                new EmailAttachment(invoice.invoiceNumber() + ".pdf", "application/pdf", invoice.pdfContent()),
                "SALES_INVOICE", invoice.id(), "invoice-delivery:" + deliveryAttemptId
        ));
    }
}
