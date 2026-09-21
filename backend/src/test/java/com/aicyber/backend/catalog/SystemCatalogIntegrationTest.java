package com.aicyber.backend.catalog;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SystemCatalogIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtService jwt;

    @Test
    void latestCatalogImportsTwelveSystemsWhileOnlyApprovedPreviewsArePublic() throws Exception {
        mockMvc.perform(get("/api/systems/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].sku").value("JON-STK-5060"))
                .andExpect(jsonPath("$[0].priceCents").value(254900))
                .andExpect(jsonPath("$[1].specifications.GPU").value("NVIDIA GeForce RTX 5060 Ti 16GB GDDR7"))
                .andExpect(jsonPath("$[3].priceCents").value(394900))
                .andExpect(jsonPath("$[0].available").isBoolean())
                .andExpect(jsonPath("$[0].availableQuantity").doesNotExist())
                .andExpect(jsonPath("$[0].plannedPreorderQuantity").doesNotExist())
                .andExpect(jsonPath("$[0].onHandQuantity").doesNotExist())
                .andExpect(jsonPath("$[0].reorderPoint").doesNotExist())
                .andExpect(jsonPath("$[0].specifications.Warranty").doesNotExist());

        mockMvc.perform(get("/api/admin/systems").header("Authorization", "Bearer " + userToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].sku").value("S1"))
                .andExpect(jsonPath("$[0].priceCents").value(2319900))
                .andExpect(jsonPath("$[0].productRange").value("Premium"))
                .andExpect(jsonPath("$[0].salesMode").value("STOCK_CHECK_REQUIRED"))
                .andExpect(jsonPath("$[0].onHandQuantity").value(0))
                .andExpect(jsonPath("$[8].sku").value("JON-STK-5060"))
                .andExpect(jsonPath("$[8].plannedPreorderQuantity").value(8))
                .andExpect(jsonPath("$[8].onHandQuantity").value(8))
                .andExpect(jsonPath("$[8].sourceRevision").value("2026-09-16"));

        Integer componentStock = jdbc.queryForObject("SELECT COALESCE(SUM(on_hand_quantity), 0) FROM inventory_balances", Integer.class);
        Integer systemStock = jdbc.queryForObject("SELECT COALESCE(SUM(on_hand_quantity), 0) FROM system_inventory_balances", Integer.class);
        org.junit.jupiter.api.Assertions.assertNotNull(componentStock);
        org.junit.jupiter.api.Assertions.assertEquals(20, systemStock);
    }

    @Test
    void onlyAdminCanCreateAnotherDraft() throws Exception {
        String body = """
                {"sku":"JON-NEW-TEST","name":"New system","description":"Draft","priceCents":259900,
                 "plannedPreorderQuantity":0,"listingOrder":9,"badge":null,"recommendedFor":"Gaming",
                 "dispatchEstimate":"10–15 business days","specifications":{"GPU":"RTX 5070"},"previewEnabled":false}
                """;
        mockMvc.perform(post("/api/admin/systems").contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/systems").header("Authorization", "Bearer " + userToken("CUSTOMER"))
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/admin/systems").header("Authorization", "Bearer " + userToken("ADMIN"))
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.onHandQuantity").value(0))
                .andExpect(jsonPath("$.previewEnabled").value(false));
        mockMvc.perform(get("/api/systems/preview"))
                .andExpect(jsonPath("$.length()").value(4));
    }

    @Test
    void incompleteSystemCannotBeShownOnSite() throws Exception {
        String body = """
                {"sku":"JON-INCOMPLETE","name":"Incomplete system","priceCents":259900,
                 "plannedPreorderQuantity":0,"listingOrder":9,"recommendedFor":"Gaming",
                 "dispatchEstimate":"10–15 business days","specifications":{"GPU":"RTX 5070"},"previewEnabled":true}
                """;
        mockMvc.perform(post("/api/admin/systems").header("Authorization", "Bearer " + userToken("ADMIN"))
                        .contentType("application/json").content(body))
                .andExpect(status().isBadRequest());
    }

    private String userToken(String role) {
        UUID id = UUID.randomUUID();
        String email = id + "@example.com";
        jdbc.update("INSERT INTO users (id, email, password_hash, display_name, role, email_verified_at) VALUES (?, ?, 'hash', 'System Test', ?, CURRENT_TIMESTAMP)", id, email, role);
        return jwt.createToken(id, email, role, 1);
    }
}
