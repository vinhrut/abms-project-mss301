package com.csms.notification.client;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class FinanceInvoiceResponse {
    private UUID invoiceId;
    private UUID apartmentId;
    private String invoiceCode;
    private LocalDate billingMonth;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    private String status;
    private String displayStatus;
}
