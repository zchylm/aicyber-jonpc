package com.aicyber.backend.admin.controller;

import com.aicyber.backend.admin.dto.AdminOrderDetailResponse;
import com.aicyber.backend.admin.dto.AdminInvoiceActionResponse;
import com.aicyber.backend.admin.dto.AdminOrderSummaryResponse;
import com.aicyber.backend.admin.dto.AdminOverviewResponse;
import com.aicyber.backend.admin.dto.AdminPageResponse;
import com.aicyber.backend.admin.dto.AdminRewardSnapshotResponse;
import com.aicyber.backend.admin.service.AdminInvoiceService;
import com.aicyber.backend.admin.service.AdminOrderService;
import com.aicyber.backend.admin.service.AdminOverviewService;
import com.aicyber.backend.admin.service.AdminRewardService;
import com.aicyber.backend.invoice.model.SalesInvoice;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminOverviewService overviewService;
    private final AdminOrderService orderService;
    private final AdminRewardService rewardService;
    private final AdminInvoiceService invoiceService;

    public AdminController(
            AdminOverviewService overviewService,
            AdminOrderService orderService,
            AdminRewardService rewardService,
            AdminInvoiceService invoiceService
    ) {
        this.overviewService = overviewService;
        this.orderService = orderService;
        this.rewardService = rewardService;
        this.invoiceService = invoiceService;
    }

    @GetMapping("/overview")
    public AdminOverviewResponse overview() {
        return overviewService.overview();
    }

    @GetMapping("/orders")
    public AdminPageResponse<AdminOrderSummaryResponse> orders(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size
    ) {
        return orderService.search(query, status, page, size);
    }

    @GetMapping("/orders/{orderId}")
    public AdminOrderDetailResponse order(@PathVariable UUID orderId) {
        try {
            return orderService.detail(orderId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @GetMapping("/rewards")
    public AdminRewardSnapshotResponse rewards() {
        return rewardService.snapshot();
    }

    @GetMapping(value = "/invoices/{invoiceId}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> invoicePdf(Authentication authentication, @PathVariable UUID invoiceId) {
        try {
            SalesInvoice invoice = invoiceService.download(UUID.fromString(authentication.getName()), invoiceId);
            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(invoice.invoiceNumber() + ".pdf", StandardCharsets.UTF_8)
                    .build();
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .header("X-Content-Type-Options", "nosniff")
                    .body(invoice.pdfContent());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }

    @PostMapping("/invoices/{invoiceId}/resend")
    public AdminInvoiceActionResponse resendInvoice(Authentication authentication, @PathVariable UUID invoiceId) {
        try {
            return invoiceService.resend(UUID.fromString(authentication.getName()), invoiceId);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        }
    }
}
