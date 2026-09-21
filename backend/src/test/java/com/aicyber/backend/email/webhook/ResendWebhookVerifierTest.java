package com.aicyber.backend.email.webhook;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResendWebhookVerifierTest {

    @Test
    void acceptsValidRawPayloadAndRejectsTampering() throws Exception {
        byte[] key = "test-webhook-secret".getBytes(StandardCharsets.UTF_8);
        String secret = "whsec_" + Base64.getEncoder().encodeToString(key);
        String id = "msg_test";
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        byte[] payload = "{\"type\":\"email.delivered\"}".getBytes(StandardCharsets.UTF_8);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        mac.update((id + "." + timestamp + ".").getBytes(StandardCharsets.UTF_8));
        String signature = "v1," + Base64.getEncoder().encodeToString(mac.doFinal(payload));

        ResendWebhookVerifier verifier = new ResendWebhookVerifier(secret);
        assertTrue(verifier.verify(id, timestamp, signature, payload));
        assertFalse(verifier.verify(id, timestamp, signature, "{}".getBytes(StandardCharsets.UTF_8)));
    }
}

