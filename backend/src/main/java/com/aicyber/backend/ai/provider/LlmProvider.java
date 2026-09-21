package com.aicyber.backend.ai.provider;

import com.aicyber.backend.ai.dto.ChatResponse;
import com.aicyber.backend.ai.dto.ChatTurn;

import java.util.List;

public interface LlmProvider {

    ChatResponse answer(String message, List<ChatTurn> history, String knowledgeContext);

    default ChatResponse answer(String message, List<ChatTurn> history) {
        return answer(message, history, "");
    }

    default ChatResponse answer(String message) {
        return answer(message, List.of());
    }
}
