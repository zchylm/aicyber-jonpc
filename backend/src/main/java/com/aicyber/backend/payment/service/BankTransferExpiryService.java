package com.aicyber.backend.payment.service;

import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.repository.PaymentRepository;
import com.aicyber.backend.payment.config.BankTransferProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aicyber.backend.reward.service.FounderDiscountService;

@Service
public class BankTransferExpiryService {
    private final PaymentRepository paymentRepository;
    private final SalesOrderService salesOrderService;
    private final BankTransferProperties properties;
    private final FounderDiscountService founderDiscounts;

    public BankTransferExpiryService(PaymentRepository paymentRepository, SalesOrderService salesOrderService, BankTransferProperties properties, FounderDiscountService founderDiscounts) {
        this.paymentRepository = paymentRepository;
        this.salesOrderService = salesOrderService;
        this.properties = properties;
        this.founderDiscounts = founderDiscounts;
    }

    @Scheduled(fixedDelayString = "${jonpc.payments.bank-transfer.expiry-check-ms:60000}")
    @Transactional
    public void expireDue() {
        if (!properties.enabled()) return;
        for (Payment payment : paymentRepository.findExpiredBankTransfers()) {
            paymentRepository.cancelExpired(payment.id());
            salesOrderService.releaseReservation(payment.orderId());
            founderDiscounts.release(payment.orderId());
        }
    }
}
