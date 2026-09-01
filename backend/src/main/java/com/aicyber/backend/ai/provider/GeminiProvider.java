package com.aicyber.backend.ai.provider;

import com.aicyber.backend.ai.dto.ChatResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.http.HttpClient;
import java.time.Duration;

import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "jonpc.ai.provider", havingValue = "gemini")
public class GeminiProvider implements LlmProvider {

    private static final String API_ROOT = "https://generativelanguage.googleapis.com/v1beta/models/";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    public GeminiProvider(
            @Value("${jonpc.ai.gemini.api-key:}") String apiKey,
            @Value("${jonpc.ai.gemini.model:gemini-3.7-flash}") String model) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(8))
                .build();
        org.springframework.http.client.JdkClientHttpRequestFactory requestFactory =
                new org.springframework.http.client.JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(35));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
        this.objectMapper = new ObjectMapper();
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public ChatResponse answer(String message) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured");
        }

        Map<String, Object> request = Map.of(
                "system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt()))),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", message))
                )),
                "generationConfig", Map.of(
                        "thinkingConfig", Map.of("thinkingLevel", "low"),
                        "maxOutputTokens", 1200
                )
        );

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(API_ROOT + model + ":generateContent")
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            int status = exception.getStatusCode().value();
            if (status == 429 || status == 500 || status == 503) {
                throw new IllegalStateException("Gemini is temporarily busy. Please try again shortly.", exception);
            }
            if (status == 401 || status == 403) {
                throw new IllegalStateException("Gemini API key was rejected. Check the IDEA environment variables.", exception);
            }
            if (status == 404) {
                throw new IllegalStateException("Gemini model was not found. Check the configured model ID.", exception);
            }
            throw new IllegalStateException("Gemini request failed with status " + status + ".", exception);
        }

        String answer = extractText(responseBody);
        return new ChatResponse(
                "JON. AI / GEMINI",
                answer,
                List.of(),
                "gemini"
        );
    }

    private String extractText(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode candidate = root.path("candidates").path(0);
            StringBuilder answer = new StringBuilder();
            for (JsonNode part : candidate.path("content").path("parts")) {
                JsonNode text = part.path("text");
                if (!text.isMissingNode() && !text.asText().isBlank()) {
                    if (!answer.isEmpty()) {
                        answer.append("\n");
                    }
                    answer.append(text.asText());
                }
            }
            if (answer.isEmpty()) {
                String blockReason = root.path("promptFeedback").path("blockReason").asText("");
                if (!blockReason.isBlank()) {
                    throw new IllegalStateException("Gemini could not answer this question: " + blockReason + ".");
                }
                throw new IllegalStateException("Gemini returned no text");
            }
            if ("MAX_TOKENS".equals(candidate.path("finishReason").asText())) {
                answer.append("\n\nThe answer was shortened by the model limit. Please ask a more specific question.");
            }
            return answer.toString().replace("\\*", "*");
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Could not parse Gemini response", exception);
        }
    }

    private String systemPrompt() {
        return """
                You are JON. AI, the hardware advisor for JON. PC.
                Give practical, accurate and concise advice about GPUs, CPUs, RAM, storage, PC workloads and compatibility.
                Explain trade-offs clearly for non-experts. Never invent exact prices, stock or specifications.
                If information is uncertain, say so. Use Australian English and AUD when discussing prices.
                Use plain text with short paragraphs and simple bullet points. Do not use Markdown emphasis.
                """;
    }
}
