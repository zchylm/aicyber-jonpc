package com.aicyber.backend.admin.dto;

import java.util.UUID;

public record AdminInvoiceActionResponse(UUID invoiceId, String invoiceNumber, String deliveryStatus) {
}
