package com.aicyber.backend;

import com.aicyber.backend.invoice.controller.LocalInvoicePreviewController;
import com.aicyber.backend.payment.controller.MockPaymentController;
import com.aicyber.backend.reward.controller.RewardDemoController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "jonpc.payments.demo-enabled=true")
class InternalDemoConfigurationIntegrationTest {
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void enablesOnlyThePaymentDemoOutsideTheLocalProfile() {
        assertFalse(applicationContext.getBeansOfType(MockPaymentController.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(RewardDemoController.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(LocalInvoicePreviewController.class).isEmpty());
    }
}
