package com.aicyber.backend.admin.service;

import com.aicyber.backend.admin.dto.AdminOrderDetailResponse;
import com.aicyber.backend.admin.dto.AdminOrderSummaryResponse;
import com.aicyber.backend.admin.dto.AdminPageResponse;
import com.aicyber.backend.admin.repository.AdminQueryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class AdminOrderService {
    private final AdminQueryRepository repository;

    public AdminOrderService(AdminQueryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public AdminPageResponse<AdminOrderSummaryResponse> search(String rawQuery, String rawStatus, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String query = rawQuery == null ? "" : rawQuery.trim().toLowerCase(Locale.ROOT);
        String status = rawStatus == null ? "" : rawStatus.trim().toUpperCase(Locale.ROOT);
        return new AdminPageResponse<>(
                repository.findOrders(query, status, safeSize, safePage * safeSize),
                repository.countOrders(query, status),
                safePage,
                safeSize
        );
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponse detail(UUID orderId) {
        return repository.findOrder(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }
}
