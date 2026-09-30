package com.aicyber.backend.email.provider;

import com.aicyber.backend.email.model.EmailAttachment;
import com.aicyber.backend.email.model.QueuedEmail;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResendEmailGatewayTest {

    @Test
    void sendsBrandedEmailWithIdempotencyAndAttachment() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> idempotency = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/emails", exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            idempotency.set(exchange.getRequestHeaders().getFirst("Idempotency-Key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"id\":\"email_123\"}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            ObjectMapper json = new ObjectMapper();
            ResendEmailGateway gateway = new ResendEmailGateway(json, HttpClient.newHttpClient(), "re_test",
                    "http://localhost:" + server.getAddress().getPort() + "/emails",
                    "JON. PC <support@jonpc.com.au>", "support@jonpc.com.au");
            QueuedEmail email = new QueuedEmail(UUID.randomUUID(), "TAX_INVOICE", "buyer@example.com", "Buyer",
                    "Your invoice", "Paid", "<p>Paid</p>",
                    new EmailAttachment("invoice.pdf", "application/pdf", "pdf".getBytes(StandardCharsets.UTF_8)),
                    "invoice-delivery:123", 1);

            assertEquals("email_123", gateway.send(email));
            assertEquals("Bearer re_test", authorization.get());
            assertEquals("invoice-delivery:123", idempotency.get());
            JsonNode body = json.readTree(requestBody.get());
            assertEquals("JON. PC <support@jonpc.com.au>", body.path("from").asText());
            assertEquals("buyer@example.com", body.path("to").get(0).asText());
            assertEquals("invoice.pdf", body.path("attachments").get(0).path("filename").asText());
        } finally {
            server.stop(0);
        }
    }
}
