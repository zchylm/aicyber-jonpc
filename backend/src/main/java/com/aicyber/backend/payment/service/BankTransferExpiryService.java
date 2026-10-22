package com.aicyber.backend.payment.service;

import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.payment.repository.PaymentRepository;
import com.aicyber.backend.payment.config.BankTransferProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BankTransferExpiryService {
    private final PaymentRepository paymentRepository;
    private final SalesOrderService salesOrderService;
    private final BankTransferProperties properties;

    public BankTransferExpiryService(PaymentRepository paymentRepository, SalesOrderService salesOrderService, BankTransferProperties properties) {
        this.paymentRepository = paymentRepository;
        this.salesOrderService = salesOrderService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${jonpc.payments.bank-transfer.expiry-check-ms:60000}")
    @Transactional
    public void expireDue() {
        if (!properties.enabled()) return;
        for (Payment payment : paymentRepository.findExpiredBankTransfers()) {
            paymentRepository.cancelExpired(payment.id());
            salesOrderService.releaseReservation(payment.orderId());
        }
    }
}
