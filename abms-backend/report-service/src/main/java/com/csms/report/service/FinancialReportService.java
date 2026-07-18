package com.csms.report.service;

import com.csms.report.dto.FinancialReportDTO;

public interface FinancialReportService {

    FinancialReportDTO generate(int month, int year);
}