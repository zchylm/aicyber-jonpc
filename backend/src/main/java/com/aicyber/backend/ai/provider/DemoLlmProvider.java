package com.aicyber.backend.ai.provider;

import com.aicyber.backend.ai.dto.ChatResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "jonpc.ai.provider", havingValue = "demo", matchIfMissing = true)
public class DemoLlmProvider implements LlmProvider {

    @Override
    public ChatResponse answer(String message) {
        return new ChatResponse(
                "JON. AI backend connected",
                "Your question reached the Spring Boot chat API. The next step is to replace this demo provider with a real GPT, Gemini or Claude provider.",
                List.of(
                        "Request received by ChatController.",
                        "ChatService routed the request to LlmProvider.",
                        "The response was returned as JSON to the frontend."
                ),
                "spring-boot-demo"
        );
    }
}
