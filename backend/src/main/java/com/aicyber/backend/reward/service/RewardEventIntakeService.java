package com.aicyber.backend.reward.service;

import com.aicyber.backend.reward.repository.RewardEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RewardEventIntakeService {
    private final RewardEventRepository repository;

    public RewardEventIntakeService(RewardEventRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean recordOrderEligible(UUID programId, UUID orderId) {
        return repository.createOrderEligibleEvent(programId, orderId);
    }
}
