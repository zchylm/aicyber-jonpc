package com.aicyber.backend.payment.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StripePropertiesTest {

    @Test
    void acceptsRestrictedLiveKeyForCheckout() {
        StripeProperties properties = properties("rk_live_example");

        assertDoesNotThrow(properties::requireCheckoutConfigured);
        assertTrue(properties.liveMode());
        assertFalse(properties.testMode());
    }

    @Test
    void acceptsRestrictedTestKeyForCheckout() {
        StripeProperties properties = properties("rk_test_example");

        assertDoesNotThrow(properties::requireCheckoutConfigured);
        assertTrue(properties.testMode());
        assertFalse(properties.liveMode());
    }

    @Test
    void rejectsUnknownKeyPrefix() {
        StripeProperties properties = properties("invalid_key");

        assertThrows(IllegalStateException.class, properties::requireCheckoutConfigured);
    }

    private StripeProperties properties(String secretKey) {
        return new StripeProperties(true, secretKey, "whsec_example", 30, "https://jonpc.com.au/");
    }
}
