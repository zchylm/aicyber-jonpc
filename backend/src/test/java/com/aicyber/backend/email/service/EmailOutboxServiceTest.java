package com.aicyber.backend.email.service;

import com.aicyber.backend.email.model.EmailDraft;
import com.aicyber.backend.email.model.QueuedEmail;
import com.aicyber.backend.email.provider.EmailGateway;
import com.aicyber.backend.email.repository.EmailOutboxRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailOutboxServiceTest {

    @Test
    void temporaryGatewayFailureBecomesRetryPending() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        EmailGateway gateway = mock(EmailGateway.class);
        UUID id = UUID.randomUUID();
        QueuedEmail email = queued(id, 1);
        when(repository.claim(id)).thenReturn(true);
        when(repository.findForDelivery(id)).thenReturn(Optional.of(email));
        when(gateway.send(email)).thenThrow(new IllegalStateException("temporary"));

        assertThrows(IllegalStateException.class, () -> new EmailOutboxService(repository, gateway).deliver(id));

        verify(repository).markRetryPending(eq(id), eq("temporary"), any());
        verify(repository, never()).markTerminalFailure(any(), any());
    }

    @Test
    void fifthGatewayFailureStopsAndScrubsInsteadOfLooping() {
        EmailOutboxRepository repository = mock(EmailOutboxRepository.class);
        EmailGateway gateway = mock(EmailGateway.class);
        UUID id = UUID.randomUUID();
        QueuedEmail email = queued(id, 5);
        when(repository.claim(id)).thenReturn(true);
        when(repository.findForDelivery(id)).thenReturn(Optional.of(email));
        when(gateway.send(email)).thenThrow(new IllegalStateException("terminal"));

        assertThrows(IllegalStateException.class, () -> new EmailOutboxService(repository, gateway).deliver(id));

        verify(repository).markTerminalFailure(id, "terminal");
        verify(repository, never()).markRetryPending(any(), any(), any());
    }

    private QueuedEmail queued(UUID id, int attempts) {
        return new QueuedEmail(id, "TEST", "test@example.com", "Test", null, "Subject", "Text", null,
                null, "test:" + id, attempts);
    }
}
