package com.aicyber.backend.email.webhook;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;

@Component
public class ResendWebhookVerifier {
    private static final long MAX_AGE_SECONDS = 300;
    private final String secret;

    public ResendWebhookVerifier(@Value("${jonpc.email.resend.webhook-secret:}") String secret) {
        this.secret = secret;
    }

    public boolean verify(String eventId, String timestamp, String signatureHeader, byte[] payload) {
        if (blank(secret) || blank(eventId) || blank(timestamp) || blank(signatureHeader) || payload == null) return false;
        try {
            long timestampSeconds = Long.parseLong(timestamp);
            if (Math.abs(Instant.now().getEpochSecond() - timestampSeconds) > MAX_AGE_SECONDS) return false;

            byte[] key = Base64.getDecoder().decode(secret.startsWith("whsec_") ? secret.substring(6) : secret);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            mac.update((eventId + "." + timestamp + ".").getBytes(StandardCharsets.UTF_8));
            byte[] expected = mac.doFinal(payload);

            for (String candidate : signatureHeader.split("\\s+")) {
                if (!candidate.startsWith("v1,")) continue;
                byte[] supplied = Base64.getDecoder().decode(candidate.substring(3));
                if (MessageDigest.isEqual(expected, supplied)) return true;
            }
            return false;
        } catch (Exception ignored) {
            return false;
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }
}

