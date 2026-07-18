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
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FinancialReportServiceImpl implements FinancialReportService {

    private final FinancialReportRepository financialReportRepository;

    @Override
    @Transactional(readOnly = true)
    public FinancialReportDTO generate(int month, int year) {
        validatePeriod(month, year);

        BigDecimal totalInvoiced =
                financialReportRepository.getTotalInvoiced(month, year);

        BigDecimal totalCollected =
                financialReportRepository.getTotalCollected(month, year);

        BigDecimal totalPending =
                financialReportRepository.getTotalPending(month, year);

        BigDecimal totalOverdue =
                financialReportRepository.getTotalOverdue(month, year);

        List<RevenueBreakdownDTO> breakdown =
                financialReportRepository.getRevenueBreakdown(
                        month,
                        year,
                        totalCollected
                );

        return new FinancialReportDTO(
                month,
                year,
                totalInvoiced,
                totalCollected,
                totalPending,
                totalOverdue,
                financialReportRepository.countAllInvoices(month, year),
                financialReportRepository.countPaidInvoices(month, year),
                financialReportRepository.countPendingInvoices(month, year),
                financialReportRepository.countOverdueInvoices(month, year),
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