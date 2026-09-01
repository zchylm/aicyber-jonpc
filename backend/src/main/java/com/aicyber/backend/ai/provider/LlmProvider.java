package com.aicyber.backend.ai.provider;

import com.aicyber.backend.ai.dto.ChatResponse;

public interface LlmProvider {

    ChatResponse answer(String message);
}
