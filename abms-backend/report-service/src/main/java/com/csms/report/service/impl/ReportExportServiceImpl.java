package com.csms.report.service.impl;

import com.csms.report.dto.FinancialReportDTO;
import com.csms.report.dto.RevenueBreakdownDTO;
import com.csms.report.service.ReportExportService;
import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FontUnderline;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
public class ReportExportServiceImpl implements ReportExportService {

    private static final NumberFormat VND_FORMAT =
            NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"));

    @Override
    public byte[] exportPdf(FinancialReportDTO report) {
        try (
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD,
                    18
            );

            document.add(new Paragraph(
                    "MONTHLY FINANCIAL REPORT",
                    titleFont
            ));

            document.add(new Paragraph(
                    "Period: %02d/%d".formatted(
                            report.month(),
                            report.year()
                    )
            ));

            document.add(new Paragraph(
                    "Generated at: " + report.generatedAt()
                            .format(DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy HH:mm"
                            ))
            ));

            document.add(new Paragraph(" "));

            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);

            addPdfRow(summaryTable, "Total invoiced",
                    formatMoney(report.totalInvoiced()));
            addPdfRow(summaryTable, "Total collected",
                    formatMoney(report.totalCollected()));
            addPdfRow(summaryTable, "Total pending",
                    formatMoney(report.totalPending()));
            addPdfRow(summaryTable, "Total overdue",
                    formatMoney(report.totalOverdue()));
            addPdfRow(summaryTable, "Total invoices",
                    String.valueOf(report.totalInvoices()));
            addPdfRow(summaryTable, "Paid invoices",
                    String.valueOf(report.paidInvoices()));
            addPdfRow(summaryTable, "Pending invoices",
                    String.valueOf(report.pendingInvoices()));
            addPdfRow(summaryTable, "Overdue invoices",
                    String.valueOf(report.overdueInvoices()));

            document.add(summaryTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Revenue breakdown",
                    FontFactory.getFont(
                            FontFactory.HELVETICA_BOLD,
                            14
                    )
            ));

            PdfPTable breakdownTable = new PdfPTable(3);
            breakdownTable.setWidthPercentage(100);

            addPdfRow(
                    breakdownTable,
                    "Fee type",
                    "Amount",
                    "Percentage"
            );

            for (RevenueBreakdownDTO item : report.revenueBreakdown()) {
                addPdfRow(
                        breakdownTable,
                        item.feeType(),
                        formatMoney(item.amount()),
                        item.percentage() + "%"
                );
            }

            document.add(breakdownTable);
            document.close();

            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to export financial report to PDF",
                    exception
            );
        }
    }

    @Override
    public byte[] exportExcel(FinancialReportDTO report) {
        try (
                Workbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream outputStream =
                        new ByteArrayOutputStream()
        ) {
            Sheet sheet = workbook.createSheet("Financial Report");

            CellStyle titleStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font titleFont =
                    workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 16);
            titleStyle.setFont(titleFont);

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont =
                    workbook.createFont();
            headerFont.setBold(true);
            headerFont.setUnderline(FontUnderline.SINGLE.getByteValue());
            headerStyle.setFont(headerFont);

            CellStyle moneyStyle = workbook.createCellStyle();
            DataFormat dataFormat = workbook.createDataFormat();
            moneyStyle.setDataFormat(
                    dataFormat.getFormat("#,##0.00 [$₫-vi-VN]")
            );

            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            titleRow.createCell(0)
                    .setCellValue("MONTHLY FINANCIAL REPORT");
            titleRow.getCell(0).setCellStyle(titleStyle);

            Row periodRow = sheet.createRow(rowIndex++);
            periodRow.createCell(0).setCellValue("Period");
            periodRow.createCell(1).setCellValue(
                    "%02d/%d".formatted(report.month(), report.year())
            );

            rowIndex++;

            rowIndex = addExcelMoneyRow(
                    sheet,
                    rowIndex,
                    "Total invoiced",
                    report.totalInvoiced(),
                    moneyStyle
            );

            rowIndex = addExcelMoneyRow(
                    sheet,
                    rowIndex,
                    "Total collected",
                    report.totalCollected(),
                    moneyStyle
            );

            rowIndex = addExcelMoneyRow(
                    sheet,
                    rowIndex,
                    "Total pending",
                    report.totalPending(),
                    moneyStyle
            );

            rowIndex = addExcelMoneyRow(
                    sheet,
                    rowIndex,
                    "Total overdue",
                    report.totalOverdue(),
                    moneyStyle
            );

            rowIndex = addExcelLongRow(
                    sheet,
                    rowIndex,
                    "Total invoices",
                    report.totalInvoices()
            );

            rowIndex = addExcelLongRow(
                    sheet,
                    rowIndex,
                    "Paid invoices",
                    report.paidInvoices()
            );

            rowIndex = addExcelLongRow(
                    sheet,
                    rowIndex,
                    "Pending invoices",
                    report.pendingInvoices()
            );

            rowIndex = addExcelLongRow(
                    sheet,
                    rowIndex,
                    "Overdue invoices",
                    report.overdueInvoices()
            );

            rowIndex++;

            Row breakdownTitle = sheet.createRow(rowIndex++);
            breakdownTitle.createCell(0)
                    .setCellValue("Revenue breakdown");
            breakdownTitle.getCell(0).setCellStyle(titleStyle);

            Row tableHeader = sheet.createRow(rowIndex++);
            tableHeader.createCell(0).setCellValue("Fee type");
            tableHeader.createCell(1).setCellValue("Amount");
            tableHeader.createCell(2).setCellValue("Percentage");

            for (int index = 0; index < 3; index++) {
                tableHeader.getCell(index).setCellStyle(headerStyle);
            }

            for (RevenueBreakdownDTO item : report.revenueBreakdown()) {
                Row row = sheet.createRow(rowIndex++);

                row.createCell(0).setCellValue(item.feeType());
                row.createCell(1).setCellValue(
                        item.amount().doubleValue()
                );
                row.getCell(1).setCellStyle(moneyStyle);

                row.createCell(2).setCellValue(
                        item.percentage().doubleValue() / 100
                );

                CellStyle percentStyle = workbook.createCellStyle();
                percentStyle.setDataFormat(
                        dataFormat.getFormat("0.00%")
                );
                row.getCell(2).setCellStyle(percentStyle);
            }

            for (int index = 0; index < 3; index++) {
                sheet.autoSizeColumn(index);
            }

            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Unable to export financial report to Excel",
                    exception
            );
        }
    }

    private void addPdfRow(
            PdfPTable table,
            String first,
            String second
    ) {
        table.addCell(first);
        table.addCell(second);
    }

    private void addPdfRow(
            PdfPTable table,
            String first,
            String second,
            String third
    ) {
        table.addCell(first);
        table.addCell(second);
        table.addCell(third);
    }

    private int addExcelMoneyRow(
            Sheet sheet,
            int rowIndex,
            String label,
            BigDecimal value,
            CellStyle moneyStyle
    ) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value.doubleValue());
        row.getCell(1).setCellStyle(moneyStyle);

        return rowIndex + 1;
    }

    private int addExcelLongRow(
            Sheet sheet,
            int rowIndex,
            String label,
            long value
    ) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(label);
        row.createCell(1).setCellValue(value);

        return rowIndex + 1;
    }

    private String formatMoney(BigDecimal value) {
        return VND_FORMAT.format(
                value == null ? BigDecimal.ZERO : value
        );
    }
}
