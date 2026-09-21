package com.aicyber.backend.admin;

import com.aicyber.backend.auth.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminSecurityIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Test
    void anonymousUserCannotAccessAdminApi() throws Exception {
        mockMvc.perform(get("/api/admin/overview"))
                .andExpect(status().isForbidden());
    }

    @Test
    void customerCannotAccessAdminApi() throws Exception {
        String token = createUser("CUSTOMER");

        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadOverviewAndOrders() throws Exception {
        String token = createUser("ADMIN");

        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").isNumber())
                .andExpect(jsonPath("$.failedRewardEvents").isNumber())
                .andExpect(jsonPath("$.ordersToday").isNumber())
                .andExpect(jsonPath("$.revenueTodayCents").isNumber())
                .andExpect(jsonPath("$.rewardsAllocatedTodayCents").isNumber())
                .andExpect(jsonPath("$.inventoryAlerts").isArray())
                .andExpect(jsonPath("$.recentActivity").isArray());

        mockMvc.perform(get("/api/admin/orders").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.total").isNumber());

        mockMvc.perform(get("/api/admin/rewards").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.campaign.maxPositions").value(50))
                .andExpect(jsonPath("$.campaign.maxLiabilityCents").value(2_500_000))
                .andExpect(jsonPath("$.campaign.committedCents").isNumber())
                .andExpect(jsonPath("$.commitments").isArray());
    }

    private String createUser(String role) {
        UUID userId = UUID.randomUUID();
        String email = userId + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name, role, email_verified_at) " +
                        "VALUES (?, ?, 'test-password-hash', 'Admin Security Test', ?, CURRENT_TIMESTAMP)",
                userId, email, role
        );
        return jwtService.createToken(userId, email, role, 1);
    }
}
