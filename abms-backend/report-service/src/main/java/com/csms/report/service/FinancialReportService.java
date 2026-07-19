package com.csms.report.service;

import com.csms.report.dto.FinancialReportDTO;

import java.util.UUID;

public interface FinancialReportService {

    FinancialReportDTO generate(int month, int year, UUID buildingId);

    default FinancialReportDTO generate(int month, int year) {
        return generate(month, year, null);
    }
}
