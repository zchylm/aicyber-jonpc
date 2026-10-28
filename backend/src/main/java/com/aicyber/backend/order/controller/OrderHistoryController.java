package com.aicyber.backend.order.controller;

import com.aicyber.backend.order.dto.OrderHistoryResponse;
import com.aicyber.backend.order.service.OrderHistoryService;
import com.aicyber.backend.order.service.OrderFulfilmentService;
import com.aicyber.backend.order.dto.OrderFulfilmentResponse;
import com.aicyber.backend.payment.stripe.StripeCheckoutGateway;
import com.aicyber.backend.reward.service.FounderDiscountService;
import org.springframework.security.core.Authentication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final OrderFulfilmentService fulfilment;
    private final FounderDiscountService founderDiscounts;
    private final StripeCheckoutGateway stripe;

    public OrderHistoryController(OrderHistoryService service, JdbcTemplate jdbc, OrderFulfilmentService fulfilment,
                                  FounderDiscountService founderDiscounts, StripeCheckoutGateway stripe) {
        this.service = service;
        this.jdbc = jdbc;
        this.fulfilment = fulfilment;
        this.founderDiscounts = founderDiscounts;
        this.stripe = stripe;
    }

    @GetMapping
    public List<OrderHistoryResponse> list(Authentication authentication) {
        return service.list(userId(authentication));
    }

    @PostMapping("/{orderId}/confirm-received")
    public OrderFulfilmentResponse confirmReceived(Authentication authentication, @PathVariable UUID orderId) {
        return fulfilment.confirmReceived(orderId, userId(authentication));
    }

    @PostMapping("/{orderId}/delivery-issue")
    public OrderFulfilmentResponse reportDeliveryIssue(
            Authentication authentication,
            @PathVariable UUID orderId,
            @RequestBody DeliveryIssueRequest request
    ) {
        return fulfilment.reportIssue(orderId, userId(authentication), request.summary());
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
            List<PaymentState> paymentStates = jdbc.query("""
                    SELECT provider, status, provider_checkout_session_id FROM payments
                    WHERE order_id = ? ORDER BY created_at DESC FOR UPDATE
                    """, (rs, row) -> new PaymentState(rs.getString(1), rs.getString(2), rs.getString(3)), order.id());
            for (PaymentState payment : paymentStates) validateCancellation(payment);
            jdbc.update("UPDATE payments SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE order_id = ? AND status IN ('CREATED', 'FAILED')", order.id());
            jdbc.update("DELETE FROM system_sale_reservations WHERE sales_order_id = ?", order.id());
            founderDiscounts.release(order.id());
            jdbc.update("UPDATE sales_orders SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE id = ?", order.id());
        }
        jdbc.update("UPDATE build_requests SET status = 'CANCELLED', updated_at = CURRENT_TIMESTAMP WHERE id = ?", requestId);
    }

    private record OrderState(UUID id, String status) {}
    private record PaymentState(String provider, String status, String checkoutSessionId) {}

    private void validateCancellation(PaymentState payment) {
        if ("MOCK".equals(payment.provider()) && List.of("CREATED", "FAILED", "CANCELLED").contains(payment.status())) return;
        if ("BANK_TRANSFER".equals(payment.provider()) && List.of("CREATED", "CANCELLED").contains(payment.status())) return;
        if ("STRIPE".equals(payment.provider()) && List.of("FAILED", "CANCELLED").contains(payment.status())) return;
        if ("STRIPE".equals(payment.provider()) && "CREATED".equals(payment.status())) {
            if (payment.checkoutSessionId() == null) return;
            StripeCheckoutGateway.CheckoutSession session = stripe.retrieve(payment.checkoutSessionId());
            if ("paid".equals(session.paymentStatus()) || "complete".equals(session.status())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stripe has already completed this payment");
            }
            if ("open".equals(session.status())) stripe.expire(payment.checkoutSessionId());
            return;
        }
        throw new ResponseStatusException(HttpStatus.CONFLICT, "This payment cannot be cancelled here");
    }

    public record DeliveryIssueRequest(String summary) {}

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
