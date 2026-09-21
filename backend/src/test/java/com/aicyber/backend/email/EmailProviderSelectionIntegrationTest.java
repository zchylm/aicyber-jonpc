package com.aicyber.backend.email;

import com.aicyber.backend.auth.email.AccountEmailSender;
import com.aicyber.backend.auth.email.TransactionalAccountEmailSender;
import com.aicyber.backend.email.provider.EmailGateway;
import com.aicyber.backend.email.provider.ResendEmailGateway;
import com.aicyber.backend.invoice.email.InvoiceEmailSender;
import com.aicyber.backend.invoice.email.TransactionalInvoiceEmailSender;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@SpringBootTest(properties = {
        "jonpc.email.provider=resend",
        "jonpc.email.dispatch-enabled=false",
        "jonpc.invoice.email-delivery-enabled=true"
})
@ActiveProfiles("local")
class EmailProviderSelectionIntegrationTest {
    @Autowired EmailGateway gateway;
    @Autowired AccountEmailSender accountEmailSender;
    @Autowired InvoiceEmailSender invoiceEmailSender;

    @Test
    void localProfileCanOptIntoRealResendDelivery() {
        assertInstanceOf(ResendEmailGateway.class, gateway);
        assertInstanceOf(TransactionalAccountEmailSender.class, accountEmailSender);
        assertInstanceOf(TransactionalInvoiceEmailSender.class, invoiceEmailSender);
    }
}

