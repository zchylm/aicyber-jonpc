package com.aicyber.backend.auth.service;

import com.aicyber.backend.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "jonpc.auth.email-verification-enabled=false")
@Transactional
class EmailVerificationDisabledIntegrationTest {

    @Autowired
    private AuthService authService;

    @Test
    void registrationRemainsUsableUntilProductionEmailDeliveryIsConfigured() {
        var response = authService.register(new RegisterRequest(
                "verification-disabled-" + UUID.randomUUID() + "@example.com",
                "secure-password",
                "Existing Production Flow"
        ));

        assertTrue(response.user().emailVerified());
    }
}
