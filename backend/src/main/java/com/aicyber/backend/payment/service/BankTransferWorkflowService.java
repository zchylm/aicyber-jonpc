package com.aicyber.backend.payment.service;

import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.service.InvoiceDeliveryService;
import com.aicyber.backend.invoice.service.InvoiceService;
import com.aicyber.backend.payment.config.BankTransferProperties;
import com.aicyber.backend.payment.dto.BankTransferResponse;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.model.PaymentSettlement;
import com.aicyber.backend.payment.repository.PaymentRepository;
import com.aicyber.backend.reward.dto.RewardEntryResponse;
import com.aicyber.backend.reward.dto.RewardMeResponse;
import com.aicyber.backend.reward.service.RewardQueryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.OffsetDateTime;

@Service
public class BankTransferWorkflowService {
    private static final Logger LOGGER = LoggerFactory.getLogger(BankTransferWorkflowService.class);

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;
    private final BankTransferSettlementService settlementService;
    private final BankTransferExpiryService expiryService;
    private final BankTransferProperties properties;
    private final RewardQueryService rewardQueryService;
    private final InvoiceService invoiceService;
    private final InvoiceDeliveryService invoiceDeliveryService;

    public BankTransferWorkflowService(
            PaymentService paymentService,
            PaymentRepository paymentRepository,
            BankTransferSettlementService settlementService,
            BankTransferExpiryService expiryService,
            BankTransferProperties properties,
            RewardQueryService rewardQueryService,
            InvoiceService invoiceService,
            InvoiceDeliveryService invoiceDeliveryService
    ) {
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
        this.settlementService = settlementService;
        this.expiryService = expiryService;
        this.properties = properties;
        this.rewardQueryService = rewardQueryService;
        this.invoiceService = invoiceService;
        this.invoiceDeliveryService = invoiceDeliveryService;
    }

    public BankTransferResponse create(UUID userId, String requestReference, String idempotencyKey) {
        expiryService.expireDue();
        properties.requireConfigured();
        Payment payment = paymentService.createBankTransfer(userId, requestReference, idempotencyKey);
        return response(userId, payment);
    }

    @Transactional
    public BankTransferResponse reportTransferred(UUID userId, UUID paymentId) {
        properties.requireConfigured();
        Payment payment = paymentRepository.lockOwned(paymentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Bank transfer payment was not found for this account"));
        if (!"BANK_TRANSFER".equals(payment.provider())) throw new IllegalArgumentException("Payment is not a bank transfer");
        if ("PROCESSING".equals(payment.status()) || "SUCCEEDED".equals(payment.status())) return response(userId, payment);
        if (!"PENDING_PAYMENT".equals(payment.orderStatus())) throw new IllegalStateException("Order is no longer awaiting payment");
        return response(userId, paymentRepository.markCustomerReported(payment.id()));
    }

    public BankTransferResponse status(UUID userId, UUID paymentId) {
        expiryService.expireDue();
        properties.requireConfigured();
        Payment payment = paymentRepository.findOwned(paymentId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Bank transfer payment was not found for this account"));
        return response(userId, payment);
    }

    public BankTransferResponse confirm(
            UUID adminId,
            UUID paymentId,
            String transactionReference,
            OffsetDateTime bankReceivedAt,
            String reviewNote
    ) {
        properties.requireConfigured();
        PaymentSettlement settlement = settlementService.confirm(
                adminId, paymentId, transactionReference, bankReceivedAt, reviewNote
        );
        if (settlement.newlySucceeded()) {
            try {
                invoiceDeliveryService.deliver(settlement.invoiceId());
            } catch (RuntimeException exception) {
                LOGGER.error("Invoice {} was issued but email delivery remains pending", settlement.invoiceId(), exception);
            }
        }
        return response(settlement.payment().userId(), settlement.payment());
    }

    public BankTransferResponse reject(UUID adminId, UUID paymentId, String reviewNote) {
        properties.requireConfigured();
        Payment payment = settlementService.reject(adminId, paymentId, reviewNote);
        return response(payment.userId(), payment);
    }

    private BankTransferResponse response(UUID userId, Payment payment) {
        SalesInvoice invoice = invoiceService.invoiceForOrder(payment.orderId()).orElse(null);
        RewardEntryResponse reward = rewardFor(userId, payment.orderReference());
        return new BankTransferResponse(
                payment.id(), payment.paymentReference(), payment.orderId(), payment.orderReference(),
                payment.amountCents(), payment.productSubtotalCents(), payment.discountCents(), payment.shippingFeeCents(),
                payment.currency(), payment.status(), payment.failureReason(),
                payment.expiresAt(), payment.customerReportedAt(), properties.bankName(), properties.accountName(),
                properties.bsb(), properties.accountNumber(), invoice == null ? null : invoice.id(),
                invoice == null ? null : invoice.invoiceNumber(), reward == null ? "NOT_ELIGIBLE" : "JOINED",
                "CREATED".equals(payment.status()) ? rewardQueryService.checkoutPreviewForOrder(userId, payment.orderId()) : null,
                reward
        );
    }

    private RewardEntryResponse rewardFor(UUID userId, String orderReference) {
        RewardMeResponse member = rewardQueryService.memberSummary(userId);
        return member.entries().stream().filter(entry -> entry.orderReference().equals(orderReference)).findFirst().orElse(null);
    }
}
