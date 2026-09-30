package com.aicyber.backend.ai.service;

import com.aicyber.backend.ai.context.KnowledgeContextProvider;
import com.aicyber.backend.ai.dto.ChatResponse;
import com.aicyber.backend.ai.dto.ChatTurn;
import com.aicyber.backend.ai.provider.LlmProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 2_000;
    private static final int MAX_HISTORY_TURNS = 10;
    private static final Set<String> ALLOWED_ROLES = Set.of("user", "assistant");
    private static final Pattern NUMBERED_BUDGET = Pattern.compile(".*\\b\\d{3,5}\\b.*");
    private static final Pattern HAN_TEXT = Pattern.compile(".*\\p{IsHan}.*");

    private final LlmProvider llmProvider;
    private final KnowledgeContextProvider knowledgeContextProvider;

    public ChatService(LlmProvider llmProvider, KnowledgeContextProvider knowledgeContextProvider) {
        this.llmProvider = llmProvider;
        this.knowledgeContextProvider = knowledgeContextProvider;
    }

    public ChatResponse answer(String message) {
        return answer(message, List.of());
    }

    public ChatResponse answer(String message, List<ChatTurn> history) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message must not be blank");
        }
        String trimmedMessage = message.trim();
        if (trimmedMessage.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("Message is too long");
        }
        List<ChatTurn> validatedHistory = validateHistory(history);
        ChatResponse guidedClarification = guidedClarification(trimmedMessage, validatedHistory);
        if (guidedClarification != null) return guidedClarification;
        return llmProvider.answer(trimmedMessage, validatedHistory, knowledgeContextProvider.currentContext());
    }

    private ChatResponse guidedClarification(String message, List<ChatTurn> history) {
        if (!history.isEmpty()) return null;
        String lower = message.toLowerCase(Locale.ROOT);
        boolean asksForRecommendation = lower.contains("what should i buy") || lower.contains("which pc")
                || lower.contains("recommend") || lower.contains("推荐") || lower.contains("买哪") || lower.contains("选哪");
        boolean hasBudget = lower.contains("budget") || lower.contains("aud") || lower.contains("$")
                || lower.contains("预算") || lower.contains("澳元") || NUMBERED_BUDGET.matcher(lower).matches();
        if (!asksForRecommendation || hasBudget) return null;
        String question = HAN_TEXT.matcher(message).matches() ? "你的预算大约是多少？" : "What budget are you working with?";
        return new ChatResponse("JON. AI", question, List.of(), "guided");
    }

    private List<ChatTurn> validateHistory(List<ChatTurn> history) {
        if (history == null || history.isEmpty()) return List.of();

        int start = Math.max(0, history.size() - MAX_HISTORY_TURNS);
        List<ChatTurn> validated = new ArrayList<>();
        for (ChatTurn turn : history.subList(start, history.size())) {
            if (turn == null || !ALLOWED_ROLES.contains(turn.role()) || turn.content() == null || turn.content().isBlank()) {
                throw new IllegalArgumentException("Conversation history is invalid");
            }
            String content = turn.content().trim();
            if (content.length() > MAX_MESSAGE_LENGTH) {
                throw new IllegalArgumentException("Conversation history is too long");
            }
            validated.add(new ChatTurn(turn.role(), content));
        }
        return List.copyOf(validated);
    }
}
