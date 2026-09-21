package com.aicyber.backend.admin.service;

import com.aicyber.backend.admin.dto.AdminInvoiceActionResponse;
import com.aicyber.backend.admin.repository.AdminAuditRepository;
import com.aicyber.backend.invoice.model.InvoiceDeliveryAttempt;
import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.repository.InvoiceRepository;
import com.aicyber.backend.invoice.service.InvoiceDeliveryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AdminInvoiceService {
    private final InvoiceRepository invoiceRepository;
    private final InvoiceDeliveryService deliveryService;
    private final AdminAuditRepository auditRepository;

    public AdminInvoiceService(
            InvoiceRepository invoiceRepository,
            InvoiceDeliveryService deliveryService,
            AdminAuditRepository auditRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.deliveryService = deliveryService;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public SalesInvoice download(UUID adminUserId, UUID invoiceId) {
        SalesInvoice invoice = invoice(invoiceId);
        auditRepository.recordInvoiceAction(adminUserId, "INVOICE_DOWNLOADED", invoice.id(), invoice.invoiceNumber());
        return invoice;
    }

    public AdminInvoiceActionResponse resend(UUID adminUserId, UUID invoiceId) {
        SalesInvoice invoice = invoice(invoiceId);
        try {
            deliveryService.redeliver(invoice.id());
            auditRepository.recordInvoiceAction(adminUserId, "INVOICE_RESENT", invoice.id(), invoice.invoiceNumber());
        } catch (RuntimeException exception) {
            auditRepository.recordInvoiceAction(adminUserId, "INVOICE_RESEND_FAILED", invoice.id(), invoice.invoiceNumber());
            throw exception;
        }
        InvoiceDeliveryAttempt delivery = invoiceRepository.latestDeliveryAttempt(invoice.id()).orElseThrow();
        return new AdminInvoiceActionResponse(invoice.id(), invoice.invoiceNumber(), delivery.status());
    }

    private SalesInvoice invoice(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
    }
}
