package com.aicyber.backend.admin.service;

import com.aicyber.backend.admin.dto.AdminOverviewResponse;
import com.aicyber.backend.admin.repository.AdminQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminOverviewService {
    private final AdminQueryRepository repository;

    public AdminOverviewService(AdminQueryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AdminOverviewResponse overview() {
        return repository.overview();
    }
}
