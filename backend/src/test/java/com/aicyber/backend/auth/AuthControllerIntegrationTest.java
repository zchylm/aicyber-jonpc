package com.aicyber.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "jonpc.auth.email-verification-enabled=false")
@AutoConfigureMockMvc
@Transactional
class AuthControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void duplicateRegistrationReturnsAUsefulSafeMessage() throws Exception {
        String email = "duplicate-" + UUID.randomUUID() + "@example.com";
        String request = registrationJson(email, "secure-password", "First Customer");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_REQUEST_INVALID"))
                .andExpect(jsonPath("$.message").value(
                        "An account with this email already exists. Log in or reset your password."
                ));
    }

    @Test
    void invalidLoginDoesNotRevealWhetherTheAccountExists() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\",\"password\":\"incorrect-password\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("AUTH_REQUEST_INVALID"))
                .andExpect(jsonPath("$.message").value("Email or password is incorrect."));
    }

    @Test
    void invalidRegistrationFieldsReturnActionableMessages() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("invalid-email", "secure-password", "Customer")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter a valid email address."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("valid@example.com", "short", "Customer")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Use at least 8 characters for your password."));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registrationJson("valid@example.com", "secure-password", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Enter a display name of 120 characters or fewer."));
    }

    private String registrationJson(String email, String password, String displayName) {
        return "{\"email\":\"" + email + "\",\"password\":\"" + password +
                "\",\"displayName\":\"" + displayName + "\"}";
    }
}
