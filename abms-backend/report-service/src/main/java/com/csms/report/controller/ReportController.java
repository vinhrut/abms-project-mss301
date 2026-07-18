package com.csms.report.controller;

import com.csms.report.dto.FinancialReportDTO;
import com.csms.report.dto.FinancialReportRequest;
import com.csms.report.service.FinancialReportService;
import com.csms.report.service.ReportExportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports/financial")
@RequiredArgsConstructor
@Validated
public class ReportController {

    private final FinancialReportService financialReportService;
    private final ReportExportService reportExportService;

    @GetMapping("/preview")
    public ResponseEntity<FinancialReportDTO> preview(
            @RequestParam @Min(value = 1, message = "Month must be between 1 and 12")
            @Max(value = 12, message = "Month must be between 1 and 12") int month,
            @RequestParam @Min(value = 2000, message = "Year must be greater than or equal to 2000")
            @Max(value = 2100, message = "Year must be less than or equal to 2100") int year,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId
    ) {
        requireReportAccess(role);
        return ResponseEntity.ok(
                financialReportService.generate(month, year, buildingId)
        );
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(
            @Valid @RequestBody FinancialReportRequest request,
            @RequestHeader("X-User-Role") String role,
            @RequestHeader(value = "X-Building-Id", required = false) UUID buildingId
    ) {
        requireReportAccess(role);
        FinancialReportDTO report =
                financialReportService.generate(
                        request.month(),
                        request.year(),
                        buildingId
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

    private void requireReportAccess(String role) {
        String normalized = role == null ? "" : role.replace("ROLE_", "").toUpperCase();
        if (!(normalized.equals("ADMIN") || normalized.equals("MANAGER")
                || normalized.equals("BUILDING_MANAGER"))) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Report Management permission is required");
        }
    }
}
