package com.aicyber.backend.ai.service;

import com.aicyber.backend.ai.dto.ChatResponse;
import com.aicyber.backend.ai.provider.LlmProvider;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final LlmProvider llmProvider;

    public ChatService(LlmProvider llmProvider) {
        this.llmProvider = llmProvider;
    }

    public ChatResponse answer(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be blank");
        }
        return llmProvider.answer(message.trim());
    }
}
