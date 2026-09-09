package com.aicyber.backend.payment.controller;

import com.aicyber.backend.payment.dto.CompleteMockPaymentRequest;
import com.aicyber.backend.payment.dto.CreateMockPaymentRequest;
import com.aicyber.backend.payment.dto.MockPaymentResponse;
import com.aicyber.backend.payment.service.MockPaymentWorkflowService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/payments/mock")
@Profile("local")
@ConditionalOnProperty(name = "jonpc.payments.demo-enabled", havingValue = "true")
public class MockPaymentController {
    private final MockPaymentWorkflowService service;

    public MockPaymentController(MockPaymentWorkflowService service) {
        this.service = service;
    }

    @PostMapping
    public MockPaymentResponse create(Authentication authentication, @RequestBody CreateMockPaymentRequest request) {
        return execute(() -> service.create(
                userId(authentication),
                request.requestReference(),
                request.idempotencyKey()
        ));
    }

    @PostMapping("/{paymentId}/complete")
    public MockPaymentResponse complete(
            Authentication authentication,
            @PathVariable UUID paymentId,
            @RequestBody CompleteMockPaymentRequest request
    ) {
        return execute(() -> service.complete(userId(authentication), paymentId, request.outcome()));
    }

    private UUID userId(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }

    private MockPaymentResponse execute(PaymentAction action) {
        try {
            return action.run();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @FunctionalInterface
    private interface PaymentAction {
        MockPaymentResponse run();
    }
}
