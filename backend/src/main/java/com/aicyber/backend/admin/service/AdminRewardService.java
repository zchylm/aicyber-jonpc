package com.aicyber.backend.admin.service;

import com.aicyber.backend.admin.dto.AdminRewardSnapshotResponse;
import com.aicyber.backend.admin.repository.AdminQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminRewardService {
    private final AdminQueryRepository repository;

    public AdminRewardService(AdminQueryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AdminRewardSnapshotResponse snapshot() {
        return repository.rewards();
    }
}
