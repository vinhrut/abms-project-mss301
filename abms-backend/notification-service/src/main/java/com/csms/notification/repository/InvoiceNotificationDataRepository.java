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
        LocalDate from = period.atDay(1);
        LocalDate to = period.plusMonths(1).atDay(1);
        RuntimeException lastFailure = null;

        for (String sql : residentQueries()) {
            try {
                List<UUID> ids = jdbcTemplate.query(sql,
                    (rs, rowNum) -> toUuid(rs.getObject("user_id")),
                    Date.valueOf(from), Date.valueOf(to));
                Set<UUID> result = new LinkedHashSet<>();
                ids.stream().filter(java.util.Objects::nonNull).forEach(result::add);
                return result;
            } catch (RuntimeException exception) {
                lastFailure = exception;
            }
        }

        throw new IllegalStateException(
            "Cannot resolve invoice recipients from the canonical billing/resident schema.", lastFailure);
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
            // Canonical schema: only residents attached to an apartment with an invoice
            // in the requested period are eligible. This prevents broadcasting to every
            // active contract when an invoice was not generated for that apartment.
            "SELECT DISTINCT ar.user_id FROM invoices i JOIN apartment_residents ar ON ar.apartment_id = i.apartment_id "
                + "WHERE i.billing_period >= ? AND i.billing_period < ? "
                + "AND UPPER(COALESCE(i.status,'UNPAID')) <> 'CANCELLED' "
                + "AND UPPER(COALESCE(ar.status,'ACTIVE')) = 'ACTIVE'",
            "SELECT DISTINCT c.user_id FROM invoices i JOIN contracts c ON c.apartment_id = i.apartment_id "
                + "WHERE i.billing_period >= ? AND i.billing_period < ? "
                + "AND UPPER(COALESCE(i.status,'UNPAID')) <> 'CANCELLED' "
                + "AND UPPER(COALESCE(c.status,'ACTIVE')) = 'ACTIVE'",
            "SELECT DISTINCT c.resident_id AS user_id FROM invoices i JOIN contracts c ON c.apartment_id = i.apartment_id "
                + "WHERE i.billing_period >= ? AND i.billing_period < ? "
                + "AND UPPER(COALESCE(i.status,'UNPAID')) <> 'CANCELLED' "
                + "AND UPPER(COALESCE(c.status,'ACTIVE')) = 'ACTIVE'"
        );
    }

    private UUID toUuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID uuid) return uuid;
        return UUID.fromString(value.toString());
    }
}
