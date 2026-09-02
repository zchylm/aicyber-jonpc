package com.aicyber.backend.build.service;

import com.aicyber.backend.build.dto.SaveBuildRequest;
import com.aicyber.backend.build.dto.SavedBuildResponse;
import com.aicyber.backend.build.repository.SavedBuildRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SavedBuildService {
    private final SavedBuildRepository repository;

    public SavedBuildService(SavedBuildRepository repository) {
        this.repository = repository;
    }

    public SavedBuildResponse save(UUID userId, SaveBuildRequest request) {
        return repository.create(userId, request);
    }

    public SavedBuildResponse update(UUID userId, UUID buildId, SaveBuildRequest request) {
        return repository.update(userId, buildId, request);
    }

    public List<SavedBuildResponse> list(UUID userId) {
        return repository.findAllByUserId(userId);
    }

    public void delete(UUID userId, UUID buildId) {
        repository.delete(userId, buildId);
    }
}
