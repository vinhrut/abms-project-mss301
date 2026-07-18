package com.csms.report.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record FinancialReportDTO(
        int month,
        int year,
        BigDecimal totalInvoiced,
        BigDecimal totalCollected,
        BigDecimal totalPending,
        BigDecimal totalOverdue,
        long totalInvoices,
        long paidInvoices,
        long pendingInvoices,
        long overdueInvoices,
        List<RevenueBreakdownDTO> revenueBreakdown,
        LocalDateTime generatedAt
) {
}