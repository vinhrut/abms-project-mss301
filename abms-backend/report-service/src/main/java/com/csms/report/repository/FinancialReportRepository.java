package com.csms.report.repository;

import com.csms.report.dto.RevenueBreakdownDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class FinancialReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public BigDecimal getTotalInvoiced(int month, int year) {
        String sql = """
                SELECT COALESCE(SUM(total_amount), 0)
                FROM invoices
                WHERE EXTRACT(MONTH FROM billing_period) = ?
                  AND EXTRACT(YEAR FROM billing_period) = ?
                  AND COALESCE(status, '') <> 'CANCELLED'
                """;

        return queryMoney(sql, month, year);
    }

    public BigDecimal getTotalCollected(int month, int year) {
        String sql = """
                SELECT COALESCE(SUM(p.amount), 0)
                FROM payments p
                INNER JOIN invoices i ON i.invoice_id = p.invoice_id
                WHERE EXTRACT(MONTH FROM i.billing_period) = ?
                  AND EXTRACT(YEAR FROM i.billing_period) = ?
                  AND UPPER(COALESCE(p.status, 'SUCCESS')) IN ('SUCCESS', 'COMPLETED', 'PAID')
                """;

        return queryMoney(sql, month, year);
    }

    public BigDecimal getTotalPending(int month, int year) {
        String sql = """
                SELECT COALESCE(SUM(
                    GREATEST(
                        COALESCE(i.total_amount, 0) -
                        COALESCE((
                            SELECT SUM(p.amount)
                            FROM payments p
                            WHERE p.invoice_id = i.invoice_id
                              AND UPPER(COALESCE(p.status, 'SUCCESS'))
                                  IN ('SUCCESS', 'COMPLETED', 'PAID')
                        ), 0),
                        0
                    )
                ), 0)
                FROM invoices i
                WHERE EXTRACT(MONTH FROM i.billing_period) = ?
                  AND EXTRACT(YEAR FROM i.billing_period) = ?
                  AND UPPER(COALESCE(i.status, 'UNPAID'))
                      IN ('UNPAID', 'PENDING', 'PARTIALLY_PAID')
                  AND (i.due_date IS NULL OR i.due_date >= CURRENT_DATE)
                """;

        return queryMoney(sql, month, year);
    }

    public BigDecimal getTotalOverdue(int month, int year) {
        String sql = """
                SELECT COALESCE(SUM(
                    GREATEST(
                        COALESCE(i.total_amount, 0) -
                        COALESCE((
                            SELECT SUM(p.amount)
                            FROM payments p
                            WHERE p.invoice_id = i.invoice_id
                              AND UPPER(COALESCE(p.status, 'SUCCESS'))
                                  IN ('SUCCESS', 'COMPLETED', 'PAID')
                        ), 0),
                        0
                    )
                ), 0)
                FROM invoices i
                WHERE EXTRACT(MONTH FROM i.billing_period) = ?
                  AND EXTRACT(YEAR FROM i.billing_period) = ?
                  AND i.due_date < CURRENT_DATE
                  AND UPPER(COALESCE(i.status, 'UNPAID')) <> 'PAID'
                """;

        return queryMoney(sql, month, year);
    }

    public long countAllInvoices(int month, int year) {
        return queryCount("""
                SELECT COUNT(*)
                FROM invoices
                WHERE EXTRACT(MONTH FROM billing_period) = ?
                  AND EXTRACT(YEAR FROM billing_period) = ?
                  AND UPPER(COALESCE(status, 'UNPAID')) <> 'CANCELLED'
                """, month, year);
    }

    public long countPaidInvoices(int month, int year) {
        return queryCount("""
                SELECT COUNT(*)
                FROM invoices
                WHERE EXTRACT(MONTH FROM billing_period) = ?
                  AND EXTRACT(YEAR FROM billing_period) = ?
                  AND UPPER(status) = 'PAID'
                """, month, year);
    }

    public long countPendingInvoices(int month, int year) {
        return queryCount("""
                SELECT COUNT(*)
                FROM invoices
                WHERE EXTRACT(MONTH FROM billing_period) = ?
                  AND EXTRACT(YEAR FROM billing_period) = ?
                  AND UPPER(COALESCE(status, 'UNPAID'))
                      IN ('UNPAID', 'PENDING', 'PARTIALLY_PAID')
                  AND (due_date IS NULL OR due_date >= CURRENT_DATE)
                """, month, year);
    }

    public long countOverdueInvoices(int month, int year) {
        return queryCount("""
                SELECT COUNT(*)
                FROM invoices
                WHERE EXTRACT(MONTH FROM billing_period) = ?
                  AND EXTRACT(YEAR FROM billing_period) = ?
                  AND due_date < CURRENT_DATE
                  AND UPPER(COALESCE(status, 'UNPAID')) <> 'PAID'
                """, month, year);
    }

    public List<RevenueBreakdownDTO> getRevenueBreakdown(
            int month,
            int year,
            BigDecimal totalCollected
    ) {
        String sql = """
                SELECT
                    COALESCE(s.service_name, 'Other') AS fee_type,
                    COALESCE(SUM(
                        CASE
                            WHEN UPPER(COALESCE(i.status, 'UNPAID')) = 'PAID'
                            THEN id.amount
                            ELSE 0
                        END
                    ), 0) AS amount
                FROM invoice_details id
                INNER JOIN invoices i ON i.invoice_id = id.invoice_id
                LEFT JOIN services s ON s.service_id = id.service_id
                WHERE EXTRACT(MONTH FROM i.billing_period) = ?
                  AND EXTRACT(YEAR FROM i.billing_period) = ?
                GROUP BY COALESCE(s.service_name, 'Other')
                ORDER BY amount DESC
                """;

        return jdbcTemplate.query(
                sql,
                (resultSet, rowNumber) -> {
                    BigDecimal amount = safeMoney(
                            resultSet.getBigDecimal("amount")
                    );

                    BigDecimal percentage = BigDecimal.ZERO;

                    if (totalCollected != null
                            && totalCollected.compareTo(BigDecimal.ZERO) > 0) {
                        percentage = amount
                                .multiply(BigDecimal.valueOf(100))
                                .divide(totalCollected, 2, RoundingMode.HALF_UP);
                    }

                    return new RevenueBreakdownDTO(
                            resultSet.getString("fee_type"),
                            amount,
                            percentage
                    );
                },
                month,
                year
        );
    }

    private BigDecimal queryMoney(String sql, int month, int year) {
        BigDecimal result = jdbcTemplate.queryForObject(
                sql,
                BigDecimal.class,
                month,
                year
        );

        return safeMoney(result);
    }

    private long queryCount(String sql, int month, int year) {
        Long result = jdbcTemplate.queryForObject(
                sql,
                Long.class,
                month,
                year
        );

        return result == null ? 0L : result;
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO
                : value.setScale(2, RoundingMode.HALF_UP);
    }
}