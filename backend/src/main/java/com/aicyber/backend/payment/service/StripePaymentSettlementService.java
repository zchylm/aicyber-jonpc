package com.aicyber.backend.payment.service;

import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.service.InvoiceService;
import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.model.PaymentSettlement;
import com.aicyber.backend.payment.repository.PaymentRepository;
import com.aicyber.backend.reward.repository.RewardDemoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class StripePaymentSettlementService {
    private final PaymentRepository payments;
    private final RewardDemoRepository rewards;
    private final InvoiceService invoices;
    private final SuccessfulPaymentSettlementService successfulSettlement;
    private final SalesOrderService orders;

    public StripePaymentSettlementService(
            PaymentRepository payments,
            RewardDemoRepository rewards,
            InvoiceService invoices,
            SuccessfulPaymentSettlementService successfulSettlement,
            SalesOrderService orders
    ) {
        this.payments = payments;
        this.rewards = rewards;
        this.invoices = invoices;
        this.successfulSettlement = successfulSettlement;
        this.orders = orders;
    }

    @Transactional
    public PaymentSettlement succeed(UUID paymentId, String paymentIntentId) {
        Payment payment = stripePayment(paymentId);
        UUID programId = rewards.ensureProgram();
        if ("SUCCEEDED".equals(payment.status())) {
            SalesInvoice invoice = invoices.issueForSuccessfulPayment(payment.orderId(), payment.id());
            return new PaymentSettlement(payment, programId, false, invoice.id());
        }
        if (!"PENDING_PAYMENT".equals(payment.orderStatus())) {
            Payment succeeded = payments.findSuccessfulByOrder(payment.orderId())
                    .orElseThrow(() -> new IllegalStateException("Sales order is no longer awaiting payment"));
            SalesInvoice invoice = invoices.issueForSuccessfulPayment(succeeded.orderId(), succeeded.id());
            return new PaymentSettlement(succeeded, programId, false, invoice.id());
        }
        if (!("CREATED".equals(payment.status()) || "PROCESSING".equals(payment.status()))) {
            throw new IllegalStateException("Stripe payment can no longer be completed");
        }
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("Stripe payment intent is missing");
        }
        Payment succeeded = payments.markStripeSucceeded(payment.id(), paymentIntentId);
        payments.cancelOtherAttempts(payment.orderId(), payment.id());
        SuccessfulPaymentSettlementService.SettlementResult result = successfulSettlement.settle(succeeded);
        return new PaymentSettlement(succeeded, result.programId(), true, result.invoiceId());
    }

    @Transactional
    public Payment markProcessing(UUID paymentId) {
        Payment payment = stripePayment(paymentId);
        if ("CREATED".equals(payment.status())) return payments.markStripeProcessing(payment.id());
        return payment;
    }

    @Transactional
    public Payment fail(UUID paymentId, String reason) {
        Payment payment = stripePayment(paymentId);
        if ("FAILED".equals(payment.status()) || "CANCELLED".equals(payment.status()) || "SUCCEEDED".equals(payment.status())) {
            return payment;
        }
        Payment failed = payments.markStripeFailed(payment.id(), reason);
        releaseIfUnreservedElsewhere(failed);
        return failed;
    }

    @Transactional
    public Payment cancel(UUID paymentId) {
        Payment payment = stripePayment(paymentId);
        if ("CANCELLED".equals(payment.status()) || "FAILED".equals(payment.status()) || "SUCCEEDED".equals(payment.status())) {
            return payment;
        }
        Payment cancelled = payments.markStripeCancelled(payment.id());
        releaseIfUnreservedElsewhere(cancelled);
        return cancelled;
    }

    private Payment stripePayment(UUID paymentId) {
        Payment payment = payments.lock(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Stripe payment was not found"));
        if (!"STRIPE".equals(payment.provider())) throw new IllegalArgumentException("Payment is not a Stripe payment");
        return payment;
    }

    private void releaseIfUnreservedElsewhere(Payment payment) {
        if (!payments.hasOtherActiveAttempt(payment.orderId(), payment.id())) {
            orders.releaseReservation(payment.orderId());
        }
    }
}
