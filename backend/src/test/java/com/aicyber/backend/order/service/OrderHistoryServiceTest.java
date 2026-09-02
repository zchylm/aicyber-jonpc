package com.aicyber.backend.order.service;

import com.aicyber.backend.order.repository.OrderHistoryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderHistoryServiceTest {

    private final OrderHistoryRepository repository = mock(OrderHistoryRepository.class);
    private final OrderHistoryService service = new OrderHistoryService(repository);

    @Test
    void listsOnlyTheRequestedUsersOrders() {
        UUID userId = UUID.randomUUID();
        when(repository.findAllByUserId(userId)).thenReturn(List.of());

        assertEquals(List.of(), service.list(userId));
        verify(repository).findAllByUserId(userId);
    }
}
