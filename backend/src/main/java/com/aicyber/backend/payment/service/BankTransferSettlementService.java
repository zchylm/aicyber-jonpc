package com.aicyber.backend.payment.service;

import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.service.InvoiceService;
import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.model.PaymentSettlement;
import com.aicyber.backend.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import com.aicyber.backend.reward.service.FounderDiscountService;

import java.util.UUID;
import java.time.OffsetDateTime;

@Service
public class BankTransferSettlementService {
    private final PaymentRepository paymentRepository;
    private final InvoiceService invoiceService;
    private final SuccessfulPaymentSettlementService successfulPaymentSettlementService;
    private final SalesOrderService salesOrderService;
    private final FounderDiscountService founderDiscounts;

    public BankTransferSettlementService(
            PaymentRepository paymentRepository,
            InvoiceService invoiceService,
            SuccessfulPaymentSettlementService successfulPaymentSettlementService,
            SalesOrderService salesOrderService,
            FounderDiscountService founderDiscounts
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceService = invoiceService;
        this.successfulPaymentSettlementService = successfulPaymentSettlementService;
        this.salesOrderService = salesOrderService;
        this.founderDiscounts = founderDiscounts;
    }

    @Transactional
    public PaymentSettlement confirm(
            UUID adminId,
            UUID paymentId,
            String transactionReference,
            OffsetDateTime bankReceivedAt,
            String reviewNote
    ) {
        Payment payment = paymentRepository.lock(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Bank transfer payment was not found"));
        if (!"BANK_TRANSFER".equals(payment.provider())) {
            throw new IllegalArgumentException("Payment is not a bank transfer");
        }
        if ("SUCCEEDED".equals(payment.status())) {
            SalesInvoice invoice = invoiceService.issueForSuccessfulPayment(payment.orderId(), payment.id());
            return new PaymentSettlement(payment, false, invoice.id());
        }
        if (!"PENDING_PAYMENT".equals(payment.orderStatus()) || !"PROCESSING".equals(payment.status())) {
            throw new IllegalStateException("Bank transfer is not awaiting admin confirmation");
        }
        String reference = requireText(transactionReference, 160, "ANZ transaction reference is required");
        OffsetDateTime receivedAt = requireReceivedAt(bankReceivedAt);
        String note = optionalText(reviewNote, 500);
        paymentRepository.lockRewardSettlementSequence();
        paymentRepository.latestBankReceivedAt().ifPresent(latest -> {
            if (receivedAt.isBefore(latest)) {
                throw new IllegalStateException(
                        "Confirm earlier ANZ receipts first. The entered receipt time is before the latest confirmed transfer."
                );
            }
        });
        Payment succeeded;
        try {
            succeeded = paymentRepository.markBankSucceeded(payment.id(), adminId, reference, receivedAt, note);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("This ANZ transaction reference is already linked to another payment", exception);
        }
        paymentRepository.cancelOtherAttempts(payment.orderId(), payment.id());
        SuccessfulPaymentSettlementService.SettlementResult result = successfulPaymentSettlementService.settle(succeeded);
        return new PaymentSettlement(succeeded, true, result.invoiceId());
    }

    @Transactional
    public Payment reject(UUID adminId, UUID paymentId, String reviewNote) {
        Payment payment = paymentRepository.lock(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Bank transfer payment was not found"));
        if (!"BANK_TRANSFER".equals(payment.provider())) {
            throw new IllegalArgumentException("Payment is not a bank transfer");
        }
        if ("FAILED".equals(payment.status())) return payment;
        if (!"PENDING_PAYMENT".equals(payment.orderStatus()) || !"PROCESSING".equals(payment.status())) {
            throw new IllegalStateException("Bank transfer is not awaiting admin review");
        }
        String note = requireText(reviewNote, 500, "Add an internal note before rejecting the transfer");
        Payment rejected = paymentRepository.markBankRejected(payment.id(), adminId, note);
        salesOrderService.releaseReservation(payment.orderId());
        founderDiscounts.release(payment.orderId());
        return rejected;
    }

    private OffsetDateTime requireReceivedAt(OffsetDateTime value) {
        if (value == null) throw new IllegalArgumentException("ANZ received time is required");
        if (value.isAfter(OffsetDateTime.now().plusMinutes(5))) {
            throw new IllegalArgumentException("ANZ received time cannot be in the future");
        }
        return value;
    }

    private String requireText(String value, int maxLength, String message) {
        if (value == null || value.isBlank() || value.trim().length() > maxLength) throw new IllegalArgumentException(message);
        return value.trim();
    }

    private String optionalText(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        if (value.trim().length() > maxLength) throw new IllegalArgumentException("Review note is too long");
        return value.trim();
    }
}
