package com.aicyber.backend.payment.service;

import com.aicyber.backend.invoice.model.SalesInvoice;
import com.aicyber.backend.invoice.service.InvoiceService;
import com.aicyber.backend.order.service.SalesOrderService;
import com.aicyber.backend.order.service.OrderFulfilmentService;
import com.aicyber.backend.payment.model.Payment;
import com.aicyber.backend.reward.service.FounderDiscountService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class SuccessfulPaymentSettlementService {
    private final SalesOrderService salesOrderService;
    private final FounderDiscountService founderDiscounts;
    private final InvoiceService invoiceService;
    private final OrderFulfilmentService fulfilmentService;

    public SuccessfulPaymentSettlementService(
            SalesOrderService salesOrderService,
            FounderDiscountService founderDiscounts,
            InvoiceService invoiceService,
            OrderFulfilmentService fulfilmentService
    ) {
        this.salesOrderService = salesOrderService;
        this.founderDiscounts = founderDiscounts;
        this.invoiceService = invoiceService;
        this.fulfilmentService = fulfilmentService;
    }

    public SettlementResult settle(Payment succeededPayment) {
        salesOrderService.markPaid(succeededPayment.orderId());
        fulfilmentService.begin(succeededPayment.orderId());
        founderDiscounts.apply(succeededPayment.orderId());
        SalesInvoice invoice = invoiceService.issueForSuccessfulPayment(succeededPayment.orderId(), succeededPayment.id());
        return new SettlementResult(invoice.id());
    }

    public record SettlementResult(UUID invoiceId) {}
}
