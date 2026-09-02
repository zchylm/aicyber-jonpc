package com.aicyber.backend.order.service;

import com.aicyber.backend.order.dto.OrderHistoryResponse;
import com.aicyber.backend.order.repository.OrderHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class OrderHistoryService {
    private final OrderHistoryRepository repository;

    public OrderHistoryService(OrderHistoryRepository repository) {
        this.repository = repository;
    }

    public List<OrderHistoryResponse> list(UUID userId) {
        return repository.findAllByUserId(userId);
    }
}
