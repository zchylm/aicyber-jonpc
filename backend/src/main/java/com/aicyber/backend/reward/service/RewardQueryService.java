package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.dto.RewardEntryResponse;
import com.aicyber.backend.reward.dto.RewardMeResponse;
import com.aicyber.backend.reward.dto.RewardPublicSummaryResponse;
import com.aicyber.backend.reward.model.RewardProgramInfo;
import com.aicyber.backend.reward.repository.RewardQueryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class RewardQueryService {
    public static final String PROGRAM_CODE = "JON_QUEUE_REWARDS";

    private final RewardQueryRepository repository;

    public RewardQueryService(RewardQueryRepository repository) {
        this.repository = repository;
    }

    public RewardPublicSummaryResponse publicSummary() {
        return repository.findProgram(PROGRAM_CODE)
                .map(repository::loadSummary)
                .orElseGet(RewardPublicSummaryResponse::unavailable);
    }

    public RewardMeResponse memberSummary(UUID userId) {
        Optional<RewardProgramInfo> program = repository.findProgram(PROGRAM_CODE);
        if (program.isEmpty()) {
            return new RewardMeResponse("PROGRAM_UNAVAILABLE", null, "AUD", List.of());
        }

        RewardProgramInfo activeProgram = program.get();
        List<RewardEntryResponse> entries = repository.findEntries(activeProgram.id(), userId);
        String state = entries.isEmpty() ? "NO_ENTRY" : "ACTIVE";
        return new RewardMeResponse(state, activeProgram.status(), activeProgram.currency(), entries);
    }
}
