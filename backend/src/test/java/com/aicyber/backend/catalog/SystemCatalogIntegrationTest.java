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

import static org.hamcrest.Matchers.contains;
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
    void gamingMemoryVariantsHaveSeparatePublicPricesAndAdminStock() throws Exception {
        var previews = mockMvc.perform(get("/api/systems/preview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12))
                .andExpect(jsonPath("$[0].available").isBoolean())
                .andExpect(jsonPath("$[0].saleEnabled").value(true))
                .andExpect(jsonPath("$[0].availableQuantity").doesNotExist())
                .andExpect(jsonPath("$[0].plannedPreorderQuantity").doesNotExist())
                .andExpect(jsonPath("$[0].onHandQuantity").doesNotExist())
                .andExpect(jsonPath("$[0].reorderPoint").doesNotExist())
                .andExpect(jsonPath("$[0].specifications.Warranty").doesNotExist());
        var admin = mockMvc.perform(get("/api/admin/systems").header("Authorization", "Bearer " + userToken("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(12));

        String[] skus = {"JON-STK-5060", "JON-STK-5060TI", "JON-STM-5070", "JON-STM-5070TI"};
        int[] prices16 = {209900, 249900, 269900, 349900};
        int[] prices32 = {229900, 269900, 289900, 369900};
        for (int i = 0; i < skus.length; i++) {
            String base = "$[?(@.sku == '" + skus[i] + "')]";
            String upgraded = "$[?(@.sku == '" + skus[i] + "-32GB')]";
            previews.andExpect(jsonPath(base + ".priceCents", contains(prices16[i])))
                    .andExpect(jsonPath(upgraded + ".priceCents", contains(prices32[i])))
                    .andExpect(jsonPath(upgraded + ".specifications.Memory", contains("32GB DDR5 (2 × 16GB)")));
            admin.andExpect(jsonPath(base + ".onHandQuantity", contains(5)))
                    .andExpect(jsonPath(upgraded + ".onHandQuantity", contains(5)))
                    .andExpect(jsonPath(upgraded + ".saleEnabled", contains(true)));
        }

        Integer componentStock = jdbc.queryForObject("SELECT COALESCE(SUM(on_hand_quantity), 0) FROM inventory_balances", Integer.class);
        Integer systemStock = jdbc.queryForObject("SELECT COALESCE(SUM(on_hand_quantity), 0) FROM system_inventory_balances", Integer.class);
        org.junit.jupiter.api.Assertions.assertNotNull(componentStock);
        org.junit.jupiter.api.Assertions.assertEquals(60, systemStock);
        org.junit.jupiter.api.Assertions.assertEquals(8, jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM system_builds s
                JOIN system_inventory_balances b ON b.system_build_id = s.id
                WHERE s.code IN (
                    'JON-STK-5060', 'JON-STK-5060TI', 'JON-STM-5070', 'JON-STM-5070TI',
                    'JON-EDGE-MINI', 'JON-EDGE-AI', 'JON-EDGE-PRO', 'JON-EDGE-MAX'
                )
                  AND s.sale_enabled = TRUE
                  AND b.on_hand_quantity = 5
                """, Integer.class));
    }

    @Test
    void onlyAdminCanCreateAnotherDraft() throws Exception {
        String body = """
                {"sku":"JON-NEW-TEST","name":"New system","description":"Draft","priceCents":259900,
                 "plannedPreorderQuantity":0,"listingOrder":9,"badge":null,"recommendedFor":"Gaming",
                 "dispatchEstimate":"10–15 business days","specifications":{"GPU":"RTX 5070"},"previewEnabled":false,"saleEnabled":false}
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
                .andExpect(jsonPath("$.length()").value(12));
    }

    @Test
    void incompleteSystemCannotBeShownOnSite() throws Exception {
        String body = """
                {"sku":"JON-INCOMPLETE","name":"Incomplete system","priceCents":259900,
                 "plannedPreorderQuantity":0,"listingOrder":9,"recommendedFor":"Gaming",
                 "dispatchEstimate":"10–15 business days","specifications":{"GPU":"RTX 5070"},"previewEnabled":true,"saleEnabled":false}
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
