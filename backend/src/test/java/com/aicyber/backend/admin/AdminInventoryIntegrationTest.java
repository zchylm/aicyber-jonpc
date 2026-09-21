package com.aicyber.backend.admin;

import com.aicyber.backend.auth.security.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
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
class AdminInventoryIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void adminCanCreateCatalogueDataAndRecordAuditableStockChanges() throws Exception {
        AdminIdentity admin = createAdmin();

        JsonNode location = responseJson(post("/api/admin/inventory/locations")
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"TEST-WH-01","name":"Test warehouse","locationType":"WAREHOUSE"}
                        """));

        JsonNode item = responseJson(post("/api/admin/inventory/items")
                .header("Authorization", "Bearer " + admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "categoryCode":"CPU",
                          "sku":"TEST-AMD-R5-7600",
                          "brand":"AMD",
                          "model":"Ryzen 5 7600",
                          "displayName":"AMD Ryzen 5 7600",
                          "description":"Inventory workspace integration test",
                          "specifications":{"socket":"AM5","cores":"6"},
                          "unitCostCents":28900,
                          "retailPriceCents":32900,
                          "currency":"AUD",
                          "status":"ACTIVE"
                        }
                        """));

        String receipt = """
                {"inventoryItemId":"%s","locationId":"%s","quantity":10,"reason":"Supplier delivery TEST-01","idempotencyKey":"%s"}
                """.formatted(item.get("id").asText(), location.get("id").asText(), UUID.randomUUID());
        mockMvc.perform(post("/api/admin/inventory/receipts")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(receipt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementType").value("RECEIPT"))
                .andExpect(jsonPath("$.onHandAfter").value(10));

        String adjustment = """
                {"inventoryItemId":"%s","locationId":"%s","quantity":-2,"reason":"Two damaged units","idempotencyKey":"%s"}
                """.formatted(item.get("id").asText(), location.get("id").asText(), UUID.randomUUID());
        mockMvc.perform(post("/api/admin/inventory/adjustments")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjustment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.onHandAfter").value(8));

        mockMvc.perform(get("/api/admin/inventory")
                        .header("Authorization", "Bearer " + admin.token())
                        .queryParam("query", "TEST-AMD-R5-7600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].sku").value("TEST-AMD-R5-7600"))
                .andExpect(jsonPath("$.items[0].onHandQuantity").value(8))
                .andExpect(jsonPath("$.items[0].availableQuantity").value(8))
                .andExpect(jsonPath("$.recentMovements[0].sku").value("TEST-AMD-R5-7600"))
                .andExpect(jsonPath("$.recentMovements[1].sku").value("TEST-AMD-R5-7600"))
                .andExpect(jsonPath("$.recentMovements[0].performedBy").value("Inventory Admin Test"))
                .andExpect(jsonPath("$.recentMovements[1].performedBy").value("Inventory Admin Test"));
    }

    @Test
    void adminCanReceiveAndAdjustWholeSystemsAndOverviewShowsLowAndSoldOut() throws Exception {
        AdminIdentity admin = createAdmin();
        UUID systemId = jdbcTemplate.queryForObject(
                "SELECT id FROM system_builds WHERE code = 'JON-STM-5070'", UUID.class);
        Integer allocated = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_sale_allocations WHERE system_build_id = ?", Integer.class, systemId);
        jdbcTemplate.update("UPDATE system_inventory_balances SET on_hand_quantity = ? WHERE system_build_id = ?", allocated + 1, systemId);

        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inventoryAlerts[?(@.sku == 'JON-STM-5070')].availableQuantity").value(1));

        String adjustmentKey = UUID.randomUUID().toString();
        String adjustment = """
                {"quantity":-1,"reason":"Stock count correction","idempotencyKey":"%s"}
                """.formatted(adjustmentKey);
        mockMvc.perform(post("/api/admin/systems/" + systemId + "/adjustments")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON).content(adjustment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementType").value("ADJUSTMENT"))
                .andExpect(jsonPath("$.quantityAfter").value(allocated));

        mockMvc.perform(get("/api/admin/overview").header("Authorization", "Bearer " + admin.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.featuredSystemsSoldOut").isNumber())
                .andExpect(jsonPath("$.inventoryAlerts[?(@.sku == 'JON-STM-5070')].availableQuantity").value(0));

        String receipt = """
                {"quantity":2,"reason":"Supplier delivery TEST-SYSTEM","idempotencyKey":"%s"}
                """.formatted(UUID.randomUUID());
        mockMvc.perform(post("/api/admin/systems/" + systemId + "/receipts")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON).content(receipt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.movementType").value("RECEIPT"))
                .andExpect(jsonPath("$.quantityAfter").value(allocated + 2));

        mockMvc.perform(post("/api/admin/systems/" + systemId + "/adjustments")
                        .header("Authorization", "Bearer " + admin.token())
                        .contentType(MediaType.APPLICATION_JSON).content(adjustment))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantityAfter").value(allocated));
        org.junit.jupiter.api.Assertions.assertEquals(2, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM system_inventory_movements WHERE system_build_id = ?", Integer.class, systemId));
    }

    private JsonNode responseJson(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request) throws Exception {
        String body = mockMvc.perform(request)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private AdminIdentity createAdmin() {
        UUID userId = UUID.randomUUID();
        String email = userId + "@example.com";
        jdbcTemplate.update(
                "INSERT INTO users (id, email, password_hash, display_name, role, email_verified_at) " +
                        "VALUES (?, ?, 'test-password-hash', 'Inventory Admin Test', 'ADMIN', CURRENT_TIMESTAMP)",
                userId, email
        );
        return new AdminIdentity(userId, jwtService.createToken(userId, email, "ADMIN", 1));
    }

    private record AdminIdentity(UUID id, String token) {
    }
}
