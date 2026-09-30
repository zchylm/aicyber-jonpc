package com.aicyber.backend.order.controller;

import com.aicyber.backend.order.dto.OrderHistoryResponse;
import com.aicyber.backend.order.service.OrderHistoryService;
import org.springframework.security.core.Authentication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = {
        "http://localhost:5174",
        "http://127.0.0.1:5174",
        "https://jonpc.com.au",
        "https://www.jonpc.com.au"
})
public class OrderHistoryController {
    private final OrderHistoryService service;
    private final JdbcTemplate jdbc;

    public OrderHistoryController(OrderHistoryService service, JdbcTemplate jdbc) {
        this.service = service;
        this.jdbc = jdbc;
    }

    @GetMapping
    public List<OrderHistoryResponse> list(Authentication authentication) {
        return service.list(userId(authentication));
    }

    @PostMapping("/requests/{requestId}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void cancel(Authentication authentication, @PathVariable UUID requestId) {
        UUID customerId = userId(authentication);
        List<String> requests = jdbc.query("SELECT status FROM build_requests WHERE id = ? AND user_id = ? FOR UPDATE",
                (rs, row) -> rs.getString(1), requestId, customerId);
        if (requests.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Request not found for this account");
        if ("CANCELLED".equals(requests.get(0))) return;

        List<OrderState> orders = jdbc.query("SELECT id, status FROM sales_orders WHERE build_request_id = ? FOR UPDATE",
                (rs, row) -> new OrderState(rs.getObject(1, UUID.class), rs.getString(2)), requestId);
        if (!orders.isEmpty()) {
            OrderState order = orders.get(0);
            if (!"PENDING_PAYMENT".equals(order.status()))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A paid order cannot be cancelled here");
            Long unsupported = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM payments WHERE order_id = ?
                      AND (provider <> 'MOCK' OR status NOT IN ('CREATED', 'FAILED', 'CANCELLED'))
                    """, Long.class, order.id());
            if (unsupported != null && unsupported > 0)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment cannot be cancelled here");
            jdbc.update("UPDATE payments SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE order_id = ? AND status IN ('CREATED', 'FAILED')", order.id());
            jdbc.update("UPDATE sales_orders SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE id = ?", order.id());
        }
        jdbc.update("UPDATE build_requests SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE id = ?", requestId);
    }

    private record OrderState(UUID id, String status) {}

    private UUID userId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Login is required");
        }
        try {
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid user identity", exception);
        }
    }
}
