package com.csms.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Eligible invoice recipient used by the monthly notification job. */
public record InvoiceRecipientTarget(
        UUID residentId,
        String residentName,
        String invoiceId,
        BigDecimal amount,
        LocalDate dueDate
) {}
