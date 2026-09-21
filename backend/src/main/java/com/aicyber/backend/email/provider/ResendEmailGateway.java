package com.aicyber.backend.email.provider;

import com.aicyber.backend.email.model.EmailAttachment;
import com.aicyber.backend.email.model.QueuedEmail;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "jonpc.email.provider", havingValue = "resend")
public class ResendEmailGateway implements EmailGateway {
    private final ObjectMapper json;
    private final HttpClient httpClient;
    private final String apiKey;
    private final String endpoint;
    private final String from;
    private final String replyTo;

    @Autowired
    public ResendEmailGateway(
            ObjectMapper json,
            @Value("${jonpc.email.resend.api-key:}") String apiKey,
            @Value("${jonpc.email.resend.base-url:https://api.resend.com}") String baseUrl,
            @Value("${jonpc.email.from}") String from,
            @Value("${jonpc.email.reply-to:}") String replyTo
    ) {
        this(json, HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(), apiKey,
                baseUrl.replaceAll("/+$", "") + "/emails", from, replyTo);
    }

    ResendEmailGateway(ObjectMapper json, HttpClient httpClient, String apiKey, String endpoint, String from, String replyTo) {
        this.json = json;
        this.httpClient = httpClient;
        this.apiKey = apiKey;
        this.endpoint = endpoint;
        this.from = from;
        this.replyTo = replyTo;
    }

    @Override
    public String send(QueuedEmail email) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("JON_PC_RESEND_API_KEY is not configured");
        }
        if (from == null || from.isBlank()) {
            throw new IllegalStateException("JON_PC_EMAIL_FROM is not configured");
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", email.idempotencyKey())
                .POST(HttpRequest.BodyPublishers.ofString(payload(email)))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Resend rejected the email (HTTP " + response.statusCode() + "): "
                        + safeProviderError(response.body()));
            }
            JsonNode body = json.readTree(response.body());
            String id = body.path("id").asText();
            if (id.isBlank()) throw new IllegalStateException("Resend response did not include an email id");
            return id;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Resend request was interrupted", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Resend could not be reached", exception);
        }
    }

    private String payload(QueuedEmail email) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("to", List.of(email.recipientEmail()));
        body.put("subject", email.subject());
        if (email.htmlBody() != null) body.put("html", email.htmlBody());
        if (email.textBody() != null) body.put("text", email.textBody());
        if (replyTo != null && !replyTo.isBlank()) body.put("reply_to", replyTo);
        body.put("tags", List.of(Map.of("name", "message_type", "value", email.messageType().toLowerCase())));
        EmailAttachment attachment = email.attachment();
        if (attachment != null) {
            List<Map<String, String>> attachments = new ArrayList<>();
            attachments.add(Map.of(
                    "filename", attachment.filename(),
                    "content", Base64.getEncoder().encodeToString(attachment.content())
            ));
            body.put("attachments", attachments);
        }
        try {
            return json.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Email payload could not be created", exception);
        }
    }

    private String safeProviderError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) return "no response body";
        try {
            JsonNode body = json.readTree(responseBody);
            String message = body.path("message").asText();
            return truncate(message.isBlank() ? "provider error" : message);
        } catch (JsonProcessingException ignored) {
            return "provider error";
        }
    }

    private String truncate(String value) {
        return value.length() <= 300 ? value : value.substring(0, 300);
    }
}
