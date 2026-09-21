package com.aicyber.backend.email.controller;

import com.aicyber.backend.email.webhook.ResendWebhookService;
import com.aicyber.backend.email.webhook.ResendWebhookVerifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/email/webhooks/resend")
public class ResendWebhookController {
    private final ResendWebhookVerifier verifier;
    private final ResendWebhookService service;

    public ResendWebhookController(ResendWebhookVerifier verifier, ResendWebhookService service) {
        this.verifier = verifier;
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<Void> receive(
            @RequestHeader("svix-id") String eventId,
            @RequestHeader("svix-timestamp") String timestamp,
            @RequestHeader("svix-signature") String signature,
            @RequestBody byte[] payload
    ) {
        if (!verifier.verify(eventId, timestamp, signature, payload)) return ResponseEntity.badRequest().build();
        service.handle(eventId, payload);
        return ResponseEntity.ok().build();
    }
}

