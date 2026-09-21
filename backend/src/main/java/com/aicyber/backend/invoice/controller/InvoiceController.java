package com.aicyber.backend.invoice.controller;

import com.aicyber.backend.invoice.dto.InvoiceResponse;
import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.service.InvoiceService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService service;

    public InvoiceController(InvoiceService service) {
        this.service = service;
    }

    @GetMapping("/{invoiceId}")
    public InvoiceResponse invoice(Authentication authentication, @PathVariable UUID invoiceId) {
        return InvoiceResponse.from(owned(authentication, invoiceId));
    }

    @GetMapping(value = "/{invoiceId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(Authentication authentication, @PathVariable UUID invoiceId) {
        SalesInvoice invoice = owned(authentication, invoiceId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(invoice.invoiceNumber() + ".pdf", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(invoice.pdfContent());
    }

    private SalesInvoice owned(Authentication authentication, UUID invoiceId) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Login is required");
        }
        try {
            return service.ownedInvoice(invoiceId, UUID.fromString(authentication.getName()));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(NOT_FOUND, "Invoice was not found", exception);
        }
    }
}
