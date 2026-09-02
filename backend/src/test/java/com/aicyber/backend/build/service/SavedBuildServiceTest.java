package com.aicyber.backend.build.service;

import com.aicyber.backend.build.dto.SaveBuildRequest;
import com.aicyber.backend.build.dto.SavedBuildResponse;
import com.aicyber.backend.build.repository.SavedBuildRepository;
import com.aicyber.backend.configurator.dto.ConfiguratorQuoteRequest;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SavedBuildServiceTest {

    @Test
    void savesNewBuildWithoutAnExistingId() {
        SavedBuildRepository repository = mock(SavedBuildRepository.class);
        SavedBuildService service = new SavedBuildService(repository);
        UUID userId = UUID.randomUUID();
        SaveBuildRequest request = request();

        service.save(userId, request);

        verify(repository).create(userId, request);
    }

    @Test
    void updatesOnlyTheSelectedBuild() {
        SavedBuildRepository repository = mock(SavedBuildRepository.class);
        SavedBuildService service = new SavedBuildService(repository);
        UUID userId = UUID.randomUUID();
        UUID buildId = UUID.randomUUID();
        SaveBuildRequest request = request();

        service.update(userId, buildId, request);

        verify(repository).update(eq(userId), eq(buildId), eq(request));
    }

    private SaveBuildRequest request() {
        ConfiguratorQuoteRequest configuration = new ConfiguratorQuoteRequest(
                "gaming", Map.of("budget", "$1,500-$2,000"),
                "ryzen-7-7700", "arc-b580", "32gb", "2tb", "b850-wifi", "650-bronze", "mid", "dual-tower-air",
                "ryzen-7-7700", "arc-b580", "32gb", "2tb", "b850-wifi", "650-bronze", "mid", "dual-tower-air"
        );
        return new SaveBuildRequest("Gaming / Arc B580", "gaming", "$1,500-$2,000", 1747, 1747, 0, configuration);
    }
}
