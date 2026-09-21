package com.aicyber.backend.invoice.service;

import com.aicyber.backend.invoice.config.InvoiceBusinessDetails;
import com.aicyber.backend.invoice.model.GstBreakdown;
import com.aicyber.backend.invoice.model.InvoiceLine;
import com.aicyber.backend.invoice.model.InvoiceSource;
import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.repository.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class InvoiceService {
    private static final ZoneId MELBOURNE = ZoneId.of("Australia/Melbourne");
    private static final int GST_RATE_BASIS_POINTS = 1000;
    private static final String FOUNDER_TERMS =
            "Separate from the amount paid. JON.PC initiates eligible cashback within 30 days after confirmed delivery.";

    private final InvoiceRepository repository;
    private final GstCalculator gstCalculator;
    private final InvoicePdfRenderer pdfRenderer;
    private final InvoiceBusinessDetails business;

    public InvoiceService(
            InvoiceRepository repository,
            GstCalculator gstCalculator,
            InvoicePdfRenderer pdfRenderer,
            InvoiceBusinessDetails business
    ) {
        this.repository = repository;
        this.gstCalculator = gstCalculator;
        this.pdfRenderer = pdfRenderer;
        this.business = business;
    }

    @Transactional
    public SalesInvoice issueForSuccessfulPayment(UUID orderId, UUID paymentId) {
        SalesInvoice existing = repository.findByOrder(orderId).orElse(null);
        if (existing != null) {
            if (!existing.paymentId().equals(paymentId)) {
                throw new IllegalStateException("Order already has an invoice for another payment");
            }
            return existing;
        }

        InvoiceSource source = repository.sourceForIssue(orderId, paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice source payment was not found"));
        validateSource(source);

        OffsetDateTime issuedAt = OffsetDateTime.now(MELBOURNE);
        String invoiceNumber = repository.nextInvoiceNumber(issuedAt.getYear());
        GstBreakdown amounts = gstCalculator.fromGstInclusiveTotal(source.paymentAmountCents());
        InvoiceLine line = new InvoiceLine(
                UUID.randomUUID(), 1, source.systemSku(),
                source.systemName() == null ? description(source.direction()) : source.systemName(), 1,
                amounts.subtotalExGstCents(), GST_RATE_BASIS_POINTS, amounts.gstCents(),
                amounts.totalCents(), true
        );
        SalesInvoice draft = invoice(
                source, invoiceNumber, issuedAt, amounts, List.of(line), null, null
        );
        byte[] pdf = pdfRenderer.render(draft);
        SalesInvoice issued = invoice(
                source, invoiceNumber, issuedAt, amounts, List.of(line), pdf, sha256(pdf)
        );
        repository.create(issued);
        return issued;
    }

    public SalesInvoice ownedInvoice(UUID invoiceId, UUID userId) {
        return repository.findOwned(invoiceId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice was not found for this account"));
    }

    public SalesInvoice invoice(UUID invoiceId) {
        return repository.findById(invoiceId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice was not found"));
    }

    public Optional<SalesInvoice> invoiceForOrder(UUID orderId) {
        return repository.findByOrder(orderId);
    }

    private SalesInvoice invoice(
            InvoiceSource source,
            String invoiceNumber,
            OffsetDateTime issuedAt,
            GstBreakdown amounts,
            List<InvoiceLine> lines,
            byte[] pdf,
            String sha256
    ) {
        return new SalesInvoice(
                UUID.randomUUID(), source.orderId(), source.paymentId(), source.userId(), invoiceNumber,
                "TAX_INVOICE", "ISSUED", source.orderReference(), source.paymentReference(),
                business.legalName(), business.tradingName(), business.abn(), business.address(),
                business.email(), business.phone(), source.buyerName(), source.buyerEmail(),
                source.buyerCustomerReference(), source.buyerLocation(), null, source.paymentCurrency(), amounts.subtotalExGstCents(),
                amounts.gstCents(), amounts.totalCents(), amounts.totalCents(), issuedAt,
                source.founderNumber(), source.founderTierName(), source.founderCashbackAmountCents(),
                source.founderNumber() == null ? null : FOUNDER_TERMS, lines, pdf, sha256
        );
    }

    private void validateSource(InvoiceSource source) {
        if (!"SUCCEEDED".equals(source.paymentStatus())) {
            throw new IllegalStateException("A tax invoice can only be issued for a successful payment");
        }
        if (!List.of("PAID", "REWARD_ELIGIBLE").contains(source.orderStatus())) {
            throw new IllegalStateException("A tax invoice can only be issued for a paid order");
        }
        if (source.orderAmountCents() != source.paymentAmountCents()) {
            throw new IllegalStateException("Payment amount does not match the sales order");
        }
        if (!source.orderCurrency().equals(source.paymentCurrency()) || !"AUD".equals(source.paymentCurrency())) {
            throw new IllegalStateException("Invoice currency must match the AUD sales order and payment");
        }
        if (source.buyerName() == null || source.buyerName().isBlank()) {
            throw new IllegalStateException("Buyer identity is required for the tax invoice");
        }
        if (source.buyerEmail() == null || source.buyerEmail().isBlank()) {
            throw new IllegalStateException("Buyer email is required for the tax invoice");
        }
        if (source.buyerCustomerReference() == null || source.buyerCustomerReference().isBlank()) {
            throw new IllegalStateException("Customer ID is required for the tax invoice");
        }
    }

    private String description(String direction) {
        if (direction == null || direction.isBlank()) return "JON. PC custom computer system";
        String normalized = direction.trim().toLowerCase(Locale.ROOT);
        return "JON. PC " + Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1) + " custom computer system";
    }

    private String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }
}
