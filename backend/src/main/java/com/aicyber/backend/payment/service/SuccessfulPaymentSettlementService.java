package com.aicyber.backend.payment.service;

import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.service.InvoiceService;
import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.reward.repository.RewardDemoRepository;
import com.aicyber.backend.reward.service.RewardEventIntakeService;
import com.aicyber.backend.reward.service.RewardEventProcessor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SuccessfulPaymentSettlementService {
    private final SalesOrderService salesOrderService;
    private final RewardDemoRepository rewardDemoRepository;
    private final RewardEventIntakeService rewardEventIntakeService;
    private final RewardEventProcessor rewardEventProcessor;
    private final InvoiceService invoiceService;

    public SuccessfulPaymentSettlementService(
            SalesOrderService salesOrderService,
            RewardDemoRepository rewardDemoRepository,
            RewardEventIntakeService rewardEventIntakeService,
            RewardEventProcessor rewardEventProcessor,
            InvoiceService invoiceService
    ) {
        this.salesOrderService = salesOrderService;
        this.rewardDemoRepository = rewardDemoRepository;
        this.rewardEventIntakeService = rewardEventIntakeService;
        this.rewardEventProcessor = rewardEventProcessor;
        this.invoiceService = invoiceService;
    }

    public SettlementResult settle(Payment succeededPayment) {
        UUID programId = rewardDemoRepository.ensureProgram();
        salesOrderService.markPaid(succeededPayment.orderId());
        salesOrderService.markRewardEligible(succeededPayment.orderId());
        if (!rewardEventIntakeService.recordOrderEligible(programId, succeededPayment.orderId())) {
            throw new IllegalStateException("Reward eligibility was already recorded for this order");
        }
        if (!rewardEventProcessor.processNext(programId)) {
            throw new IllegalStateException("Founder cashback commitment was not created");
        }
        SalesInvoice invoice = invoiceService.issueForSuccessfulPayment(succeededPayment.orderId(), succeededPayment.id());
        return new SettlementResult(programId, invoice.id());
    }

    public record SettlementResult(UUID programId, UUID invoiceId) {}
}
