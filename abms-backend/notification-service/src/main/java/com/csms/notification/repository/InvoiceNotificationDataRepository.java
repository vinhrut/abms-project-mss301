package com.csms.notification.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InvoiceNotificationDataRepository {
    private final JdbcTemplate jdbcTemplate;

    public int countInvoices(YearMonth period) {
        LocalDate from = period.atDay(1);
        LocalDate to = period.plusMonths(1).atDay(1);

        for (String sql : invoiceCountQueries()) {
            try {
                Integer count = jdbcTemplate.queryForObject(sql, Integer.class, Date.valueOf(from), Date.valueOf(to));
                return count == null ? 0 : count;
            } catch (RuntimeException ignored) {
                // Try the next schema variant. See README for how to replace with one exact query.
            }
        }
        throw new IllegalStateException("Cannot query invoices. Update InvoiceNotificationDataRepository SQL to match your schema.");
    }

    public Set<UUID> findEligibleResidentIds(YearMonth period) {
        LocalDate lastDay = period.atEndOfMonth();
        Set<UUID> result = new LinkedHashSet<>();

        for (String sql : residentQueries()) {
            try {
                List<UUID> ids = jdbcTemplate.query(sql,
                    (rs, rowNum) -> rs.getObject("user_id", UUID.class),
                    Date.valueOf(lastDay), Date.valueOf(lastDay));
                ids.stream().filter(java.util.Objects::nonNull).forEach(result::add);
                if (!result.isEmpty()) return result;
            } catch (RuntimeException ignored) {
                // Try fallback schema.
            }
        }

        // Fallback for projects that have apartment_residents but contracts are not implemented yet.
        try {
            List<UUID> ids = jdbcTemplate.query(
                "SELECT DISTINCT user_id FROM apartment_residents WHERE UPPER(COALESCE(status,'ACTIVE')) = 'ACTIVE'",
                (rs, rowNum) -> rs.getObject("user_id", UUID.class));
            ids.stream().filter(java.util.Objects::nonNull).forEach(result::add);
        } catch (RuntimeException ignored) {
            // handled below
        }

        if (result.isEmpty()) {
            throw new IllegalStateException("No eligible residents found or contract schema is incompatible.");
        }
        return result;
    }

    private List<String> invoiceCountQueries() {
        return List.of(
            "SELECT COUNT(*) FROM invoices WHERE billing_period >= ? AND billing_period < ? AND UPPER(COALESCE(status,'UNPAID')) <> 'CANCELLED'",
            "SELECT COUNT(*) FROM invoices WHERE issue_date >= ? AND issue_date < ? AND UPPER(COALESCE(status,'UNPAID')) <> 'CANCELLED'",
            "SELECT COUNT(*) FROM invoices WHERE created_at >= ? AND created_at < ? AND UPPER(COALESCE(status,'UNPAID')) <> 'CANCELLED'"
        );
    }

    private List<String> residentQueries() {
        return List.of(
            "SELECT DISTINCT user_id FROM contracts WHERE UPPER(COALESCE(status,'ACTIVE')) = 'ACTIVE' AND start_date <= ? AND end_date >= ?",
            "SELECT DISTINCT resident_id AS user_id FROM contracts WHERE UPPER(COALESCE(status,'ACTIVE')) = 'ACTIVE' AND start_date <= ? AND end_date >= ?"
        );
    }
}
