package com.csms.report.service.impl;

import com.csms.report.dto.FinancialReportDTO;
import com.csms.report.dto.RevenueBreakdownDTO;
import com.csms.report.repository.FinancialReportRepository;
import com.csms.report.service.FinancialReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private final FinancialReportRepository financialReportRepository;

    @Override
    @Transactional(readOnly = true)
    public FinancialReportDTO generate(int month, int year, UUID buildingId) {
        validatePeriod(month, year);
        LocalDate asOfDate = YearMonth.of(year, month).atEndOfMonth();

        BigDecimal totalInvoiced =
                financialReportRepository.getTotalInvoiced(month, year, buildingId);

        BigDecimal totalCollected =
                financialReportRepository.getTotalCollected(month, year, buildingId);

        BigDecimal totalPending =
                financialReportRepository.getTotalPending(month, year, asOfDate, buildingId);

        BigDecimal totalOverdue =
                financialReportRepository.getTotalOverdue(month, year, asOfDate, buildingId);

        List<RevenueBreakdownDTO> breakdown =
                financialReportRepository.getRevenueBreakdown(
                        month,
                        year,
                        totalCollected,
                        buildingId
                );

        return new FinancialReportDTO(
                month,
                year,
                asOfDate,
                totalInvoiced,
                totalCollected,
                totalPending,
                totalOverdue,
                financialReportRepository.countAllInvoices(month, year, buildingId),
                financialReportRepository.countPaidInvoices(month, year, buildingId),
                financialReportRepository.countPendingInvoices(month, year, asOfDate, buildingId),
                financialReportRepository.countOverdueInvoices(month, year, asOfDate, buildingId),
                breakdown,
                LocalDateTime.now()
        );
    }

    private void validatePeriod(int month, int year) {
        YearMonth requestedPeriod = YearMonth.of(year, month);
        YearMonth maximumAllowedPeriod = YearMonth.now().plusMonths(1);

        if (requestedPeriod.isAfter(maximumAllowedPeriod)) {
            throw new IllegalArgumentException(
                    "Report period cannot be later than next month"
            );
        }
    }
}
