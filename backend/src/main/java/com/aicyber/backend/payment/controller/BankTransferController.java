package com.aicyber.backend.payment.controller;

import com.aicyber.backend.payment.dto.BankTransferResponse;
import com.aicyber.backend.payment.dto.CreateBankTransferRequest;
import com.aicyber.backend.payment.service.BankTransferWorkflowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments/bank-transfer")
@ConditionalOnProperty(name = "jonpc.payments.bank-transfer.enabled", havingValue = "true")
public class BankTransferController {
    private final BankTransferWorkflowService service;

    public BankTransferController(BankTransferWorkflowService service) { this.service = service; }

    @PostMapping
    public BankTransferResponse create(Authentication authentication, @RequestBody CreateBankTransferRequest request) {
        return execute(() -> service.create(userId(authentication), request.requestReference(), request.idempotencyKey()));
    }

    @PostMapping("/{paymentId}/reported")
    public BankTransferResponse report(Authentication authentication, @PathVariable UUID paymentId) {
        return execute(() -> service.reportTransferred(userId(authentication), paymentId));
    }

    @GetMapping("/{paymentId}")
    public BankTransferResponse status(Authentication authentication, @PathVariable UUID paymentId) {
        return execute(() -> service.status(userId(authentication), paymentId));
    }

    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }

    private BankTransferResponse execute(Action action) {
        try { return action.run(); }
        catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @FunctionalInterface private interface Action { BankTransferResponse run(); }
}
