package com.aicyber.backend.order.service;

import com.aicyber.backend.order.model.SalesOrder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Transactional
class SalesOrderServiceIntegrationTest {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SalesOrderService service;

    @Test
    void convertsLatestBuildRequestAndMovesThroughEligibilityStates() {
        UUID userId = createUser();
        UUID buildRequestId = createBuildRequest(userId, 2_099);

        SalesOrder created = service.createFromLatestBuildRequest(userId);

        assertEquals(userId, created.userId());
        assertEquals(buildRequestId, created.buildRequestId());
        assertEquals(209_900, created.amountCents());
        assertEquals("PENDING_PAYMENT", created.status());

        SalesOrder paid = service.markPaid(created.id());
        assertEquals("PAID", paid.status());
        assertNotNull(paid.paidAt());

        SalesOrder eligible = service.markRewardEligible(created.id());
        assertEquals("REWARD_ELIGIBLE", eligible.status());
        assertNotNull(eligible.rewardEligibleAt());
    }

    @Test
    void cannotConvertTheSameBuildRequestTwice() {
        UUID userId = createUser();
        createBuildRequest(userId, 1_599);

        service.createFromLatestBuildRequest(userId);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createFromLatestBuildRequest(userId)
        );
        assertEquals("Submit a build request before creating a demo sales order", exception.getMessage());
    }

    private UUID createUser() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name) VALUES (?, ?, ?, ?)",
                userId, userId + "@example.com", "test-password-hash", "Sales Order Test User"
        );
        return userId;
    }

    private UUID createBuildRequest(UUID userId, int estimatedPrice) {
        UUID requestId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO build_requests " +
                        "(id, user_id, request_reference, name, email, location, direction, estimated_price, " +
                        "recommended_baseline, configuration_snapshot) " +
                        "VALUES (?, ?, ?, 'Test User', ?, 'Melbourne', 'gaming', ?, ?, '{\"answers\":{\"systemSku\":\"TEST-DEMO\"}}'::jsonb)",
                requestId,
                userId,
                "REQ-" + requestId,
                userId + "@example.com",
                estimatedPrice,
                estimatedPrice
        );
        return requestId;
    }
}
