package com.aicyber.backend.payment.controller;

import com.aicyber.backend.payment.dto.CreateStripeCheckoutRequest;
import com.aicyber.backend.payment.dto.StripePaymentResponse;
import com.aicyber.backend.payment.service.StripePaymentWorkflowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments/stripe")
@ConditionalOnProperty(name = "jonpc.payments.stripe.enabled", havingValue = "true")
public class StripePaymentController {
    private final StripePaymentWorkflowService service;

    public StripePaymentController(StripePaymentWorkflowService service) {
        this.service = service;
    }

    @PostMapping
    public StripePaymentResponse create(Authentication authentication, @RequestBody CreateStripeCheckoutRequest request) {
        return execute(() -> service.create(userId(authentication), request.requestReference(), request.idempotencyKey()));
    }

    @GetMapping("/{paymentId}")
    public StripePaymentResponse status(Authentication authentication, @PathVariable UUID paymentId) {
        return execute(() -> service.status(userId(authentication), paymentId));
    }

    @PostMapping("/webhook")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
        try {
            service.handleWebhook(payload, signature);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    private UUID userId(Authentication authentication) { return UUID.fromString(authentication.getName()); }

    private StripePaymentResponse execute(Action action) {
        try { return action.run(); }
        catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @FunctionalInterface private interface Action { StripePaymentResponse run(); }
}
