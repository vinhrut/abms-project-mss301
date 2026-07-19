package com.csms.report.service;

import com.csms.report.dto.FinancialReportDTO;

public interface ReportExportService {

    byte[] exportPdf(FinancialReportDTO report);

    byte[] exportExcel(FinancialReportDTO report);
}