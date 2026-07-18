package com.csms.report.controller;

import com.csms.report.dto.FinancialReportDTO;
import com.csms.report.dto.FinancialReportRequest;
import com.csms.report.service.FinancialReportService;
import com.csms.report.service.ReportExportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports/financial")
@RequiredArgsConstructor
public class ReportController {

    private final FinancialReportService financialReportService;
    private final ReportExportService reportExportService;

    @GetMapping("/preview")
    public ResponseEntity<FinancialReportDTO> preview(
            @RequestParam int month,
            @RequestParam int year
    ) {
        return ResponseEntity.ok(
                financialReportService.generate(month, year)
        );
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(
            @Valid @RequestBody FinancialReportRequest request
    ) {
        FinancialReportDTO report =
                financialReportService.generate(
                        request.month(),
                        request.year()
                );

        String format = request.normalizedFormat();

        return switch (format) {
            case "PDF" -> buildFileResponse(
                    reportExportService.exportPdf(report),
                    createFileName(report, "pdf"),
                    MediaType.APPLICATION_PDF
            );

            case "EXCEL", "XLSX" -> buildFileResponse(
                    reportExportService.exportExcel(report),
                    createFileName(report, "xlsx"),
                    MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    )
            );

            default -> throw new IllegalArgumentException(
                    "Format must be PDF, EXCEL or XLSX"
            );
        };
    }

    private ResponseEntity<byte[]> buildFileResponse(
            byte[] file,
            String fileName,
            MediaType mediaType
    ) {
        ContentDisposition disposition =
                ContentDisposition.attachment()
                        .filename(fileName, StandardCharsets.UTF_8)
                        .build();

        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(file.length)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        disposition.toString()
                )
                .body(file);
    }

    private String createFileName(
            FinancialReportDTO report,
            String extension
    ) {
        return "financial-report-%d-%02d.%s".formatted(
                report.year(),
                report.month(),
                extension
        );
    }
}