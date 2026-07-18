package com.csms.report.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record FinancialReportRequest(
        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        Integer month,

        @NotNull(message = "Year is required")
        @Min(value = 2000, message = "Year must be greater than or equal to 2000")
        @Max(value = 2100, message = "Year must be less than or equal to 2100")
        Integer year,

        @NotBlank(message = "Format is required")
        @Pattern(regexp = "(?i)PDF|EXCEL|XLSX", message = "Format must be PDF, EXCEL or XLSX")
        String format
) {
    public String normalizedFormat() {
        return format.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
