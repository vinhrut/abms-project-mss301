package com.csms.report.dto;

import java.math.BigDecimal;

public record RevenueBreakdownDTO(
        String feeType,
        BigDecimal amount,
        BigDecimal percentage
) {
}