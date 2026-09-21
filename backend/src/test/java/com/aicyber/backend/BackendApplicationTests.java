package com.aicyber.backend;

import com.aicyber.backend.invoice.controller.LocalInvoicePreviewController;
import com.aicyber.backend.reward.controller.RewardDemoController;
import com.aicyber.backend.payment.controller.MockPaymentController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class BackendApplicationTests {
    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
    }

    @Test
    void demoControllerIsAbsentWithoutLocalProfile() {
        assertTrue(applicationContext.getBeansOfType(RewardDemoController.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(MockPaymentController.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(LocalInvoicePreviewController.class).isEmpty());
    }

}
