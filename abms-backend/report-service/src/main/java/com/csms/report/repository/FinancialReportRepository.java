package com.csms.report.repository;

import com.csms.report.dto.RevenueBreakdownDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class FinancialReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public BigDecimal getTotalInvoiced(int month, int year, UUID buildingId) {
        String sql = """
                SELECT COALESCE(SUM(i.total_amount), 0)
                FROM invoices i
                WHERE MONTH(i.billing_period) = ?
                  AND YEAR(i.billing_period) = ?
                  AND COALESCE(i.status, '') <> 'CANCELLED'
                """ + scope("i", buildingId);
        return queryMoney(sql, args(month, year, buildingId));
    }

    /** Collected revenue is cash-basis: payment date, not invoice billing period. */
    public BigDecimal getTotalCollected(int month, int year, UUID buildingId) {
        String sql = """
                SELECT COALESCE(SUM(p.amount), 0)
                FROM payments p
                INNER JOIN invoices i ON i.invoice_id = p.invoice_id
                WHERE p.paid_at >= ?
                  AND p.paid_at < ?
                  AND UPPER(COALESCE(p.status, 'SUCCESS')) IN ('SUCCESS', 'COMPLETED', 'PAID')
                """ + scope("i", buildingId);
        LocalDate from = LocalDate.of(year, month, 1);
        return queryMoney(sql, args(from, from.plusMonths(1), buildingId));
    }

    public BigDecimal getTotalPending(int month, int year, LocalDate asOfDate, UUID buildingId) {
        String sql = """
                SELECT COALESCE(SUM(
                    CASE WHEN COALESCE(i.total_amount, 0) - COALESCE((
                        SELECT SUM(p.amount)
                        FROM payments p
                        WHERE p.invoice_id = i.invoice_id
                          AND UPPER(COALESCE(p.status, 'SUCCESS')) IN ('SUCCESS', 'COMPLETED', 'PAID')
                    ), 0) > 0
                    THEN COALESCE(i.total_amount, 0) - COALESCE((
                        SELECT SUM(p.amount)
                        FROM payments p
                        WHERE p.invoice_id = i.invoice_id
                          AND UPPER(COALESCE(p.status, 'SUCCESS')) IN ('SUCCESS', 'COMPLETED', 'PAID')
                    ), 0)
                    ELSE 0 END
                ), 0)
                FROM invoices i
                WHERE MONTH(i.billing_period) = ?
                  AND YEAR(i.billing_period) = ?
                  AND UPPER(COALESCE(i.status, 'UNPAID')) IN ('UNPAID', 'PENDING', 'PARTIALLY_PAID')
                  AND (i.due_date IS NULL OR i.due_date >= ?)
                """ + scope("i", buildingId);
        return queryMoney(sql, args(month, year, asOfDate, buildingId));
    }

    public BigDecimal getTotalOverdue(int month, int year, LocalDate asOfDate, UUID buildingId) {
        String sql = """
                SELECT COALESCE(SUM(
                    CASE WHEN COALESCE(i.total_amount, 0) - COALESCE((
                        SELECT SUM(p.amount)
                        FROM payments p
                        WHERE p.invoice_id = i.invoice_id
                          AND UPPER(COALESCE(p.status, 'SUCCESS')) IN ('SUCCESS', 'COMPLETED', 'PAID')
                    ), 0) > 0
                    THEN COALESCE(i.total_amount, 0) - COALESCE((
                        SELECT SUM(p.amount)
                        FROM payments p
                        WHERE p.invoice_id = i.invoice_id
                          AND UPPER(COALESCE(p.status, 'SUCCESS')) IN ('SUCCESS', 'COMPLETED', 'PAID')
                    ), 0)
                    ELSE 0 END
                ), 0)
                FROM invoices i
                WHERE MONTH(i.billing_period) = ?
                  AND YEAR(i.billing_period) = ?
                  AND i.due_date < ?
                  AND UPPER(COALESCE(i.status, 'UNPAID')) <> 'PAID'
                """ + scope("i", buildingId);
        return queryMoney(sql, args(month, year, asOfDate, buildingId));
    }

    public long countAllInvoices(int month, int year, UUID buildingId) {
        return queryCount("""
                SELECT COUNT(*)
                FROM invoices i
                WHERE MONTH(i.billing_period) = ?
                  AND YEAR(i.billing_period) = ?
                  AND UPPER(COALESCE(i.status, 'UNPAID')) <> 'CANCELLED'
                """ + scope("i", buildingId), args(month, year, buildingId));
    }

    public long countPaidInvoices(int month, int year, UUID buildingId) {
        return queryCount("""
                SELECT COUNT(*)
                FROM invoices i
                WHERE MONTH(i.billing_period) = ?
                  AND YEAR(i.billing_period) = ?
                  AND UPPER(COALESCE(i.status, 'UNPAID')) = 'PAID'
                """ + scope("i", buildingId), args(month, year, buildingId));
    }

    public long countPendingInvoices(int month, int year, LocalDate asOfDate, UUID buildingId) {
        return queryCount("""
                SELECT COUNT(*)
                FROM invoices i
                WHERE MONTH(i.billing_period) = ?
                  AND YEAR(i.billing_period) = ?
                  AND UPPER(COALESCE(i.status, 'UNPAID')) IN ('UNPAID', 'PENDING', 'PARTIALLY_PAID')
                  AND (i.due_date IS NULL OR i.due_date >= ?)
                """ + scope("i", buildingId), args(month, year, asOfDate, buildingId));
    }

    public long countOverdueInvoices(int month, int year, LocalDate asOfDate, UUID buildingId) {
        return queryCount("""
                SELECT COUNT(*)
                FROM invoices i
                WHERE MONTH(i.billing_period) = ?
                  AND YEAR(i.billing_period) = ?
                  AND i.due_date < ?
                  AND UPPER(COALESCE(i.status, 'UNPAID')) <> 'PAID'
                """ + scope("i", buildingId), args(month, year, asOfDate, buildingId));
    }

    /** Allocates collected cash proportionally to invoice details for partial payments. */
    public List<RevenueBreakdownDTO> getRevenueBreakdown(int month, int year,
                                                          BigDecimal totalCollected,
                                                          UUID buildingId) {
        String sql = """
                SELECT COALESCE(s.service_name, 'Other') AS fee_type,
                       COALESCE(SUM(id.amount * CASE
                           WHEN COALESCE(collected.amount, 0) / NULLIF(i.total_amount, 0) > 1 THEN 1
                           WHEN COALESCE(collected.amount, 0) / NULLIF(i.total_amount, 0) < 0 THEN 0
                           ELSE COALESCE(collected.amount, 0) / NULLIF(i.total_amount, 0)
                       END), 0) AS amount
                FROM invoice_details id
                INNER JOIN invoices i ON i.invoice_id = id.invoice_id
                LEFT JOIN services s ON s.service_id = id.service_id
                LEFT JOIN (
                    SELECT p.invoice_id, SUM(p.amount) AS amount
                    FROM payments p
                    WHERE p.paid_at >= ?
                      AND p.paid_at < ?
                      AND UPPER(COALESCE(p.status, 'SUCCESS')) IN ('SUCCESS', 'COMPLETED', 'PAID')
                    GROUP BY p.invoice_id
                ) collected ON collected.invoice_id = i.invoice_id
                WHERE UPPER(COALESCE(i.status, 'UNPAID')) <> 'CANCELLED'
                """ + scope("i", buildingId)
                + " GROUP BY COALESCE(s.service_name, 'Other') ORDER BY amount DESC";

        LocalDate from = LocalDate.of(year, month, 1);
        Object[] parameters = args(from, from.plusMonths(1), buildingId);
        return jdbcTemplate.query(sql, parameters, (resultSet, rowNumber) -> {
            BigDecimal amount = safeMoney(resultSet.getBigDecimal("amount"));
            BigDecimal percentage = BigDecimal.ZERO;
            if (totalCollected != null && totalCollected.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount.multiply(BigDecimal.valueOf(100))
                        .divide(totalCollected, 2, RoundingMode.HALF_UP);
            }
            return new RevenueBreakdownDTO(resultSet.getString("fee_type"), amount, percentage);
        });
    }

    private String scope(String invoiceAlias, UUID buildingId) {
        if (buildingId == null) return "";
        return " AND EXISTS (SELECT 1 FROM apartments a WHERE a.apartment_id = "
                + invoiceAlias + ".apartment_id AND a.building_id = ?)";
    }

    private BigDecimal queryMoney(String sql, Object[] parameters) {
        BigDecimal result = jdbcTemplate.queryForObject(sql, parameters, BigDecimal.class);
        return safeMoney(result);
    }

    private long queryCount(String sql, Object[] parameters) {
        Long result = jdbcTemplate.queryForObject(sql, parameters, Long.class);
        return result == null ? 0L : result;
    }

    private Object[] args(int month, int year, UUID buildingId) {
        List<Object> values = new ArrayList<>(List.of(month, year));
        addBuilding(values, buildingId);
        return values.toArray();
    }

    private Object[] args(int month, int year, LocalDate asOfDate, UUID buildingId) {
        List<Object> values = new ArrayList<>(List.of(month, year, java.sql.Date.valueOf(asOfDate)));
        addBuilding(values, buildingId);
        return values.toArray();
    }

    private Object[] args(LocalDate from, LocalDate to, UUID buildingId) {
        List<Object> values = new ArrayList<>(List.of(
                java.sql.Date.valueOf(from), java.sql.Date.valueOf(to)));
        addBuilding(values, buildingId);
        return values.toArray();
    }

    private void addBuilding(List<Object> values, UUID buildingId) {
        if (buildingId != null) values.add(buildingId.toString());
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }
}
