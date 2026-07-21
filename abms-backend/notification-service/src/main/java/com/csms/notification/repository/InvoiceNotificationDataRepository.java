package com.csms.notification.repository;

import com.csms.notification.client.ApartmentResidentClientResponse;
import com.csms.notification.client.ApartmentServiceClient;
import com.csms.notification.client.FinanceInvoiceResponse;
import com.csms.notification.client.FinanceServiceClient;
import com.csms.notification.dto.InvoiceRecipientTarget;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Resolves monthly invoice notification targets from automated-finance-service
 * (+ apartment-service for ACTIVE residents), not from notification_db JDBC.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class InvoiceNotificationDataRepository {
    private static final UUID RESIDENT_A101 = UUID.fromString("00000000-0000-0000-0000-000000001101");
    private static final UUID RESIDENT_A102 = UUID.fromString("00000000-0000-0000-0000-000000001102");
    private static final UUID RESIDENT_B101 = UUID.fromString("00000000-0000-0000-0000-000000001201");

    private final FinanceServiceClient financeServiceClient;
    private final ApartmentServiceClient apartmentServiceClient;

    public int countInvoices(YearMonth period) {
        return (int) findEligibleTargets(period).stream().map(InvoiceRecipientTarget::invoiceId).distinct().count();
    }

    public Set<UUID> findEligibleResidentIds(YearMonth period) {
        Set<UUID> ids = new LinkedHashSet<>();
        findEligibleTargets(period).forEach(target -> ids.add(target.residentId()));
        return ids;
    }

    public List<InvoiceRecipientTarget> findEligibleTargets(YearMonth period) {
        LocalDate defaultDueDate = period.plusMonths(1).atDay(15);
        List<FinanceInvoiceResponse> invoices = financeServiceClient.getInvoicesByBillingMonth(period);

        if (invoices.isEmpty()) {
            log.warn("No invoices from finance-service for {}. Falling back to demo recipients.", period);
            return demoTargets(period, defaultDueDate);
        }

        Map<String, InvoiceRecipientTarget> unique = new LinkedHashMap<>();
        for (FinanceInvoiceResponse invoice : invoices) {
            if (invoice.getApartmentId() == null || invoice.getInvoiceId() == null) {
                continue;
            }
            if (isCancelled(invoice.getStatus())) {
                continue;
            }

            List<ApartmentResidentClientResponse> residents =
                    apartmentServiceClient.getResidentsByApartmentId(invoice.getApartmentId());

            String invoiceRef = blankToNull(invoice.getInvoiceCode()) != null
                    ? invoice.getInvoiceCode()
                    : invoice.getInvoiceId().toString();
            BigDecimal amount = invoice.getRemainingAmount() != null
                    ? invoice.getRemainingAmount()
                    : (invoice.getTotalAmount() == null ? BigDecimal.ZERO : invoice.getTotalAmount());

            for (ApartmentResidentClientResponse resident : residents) {
                if (resident.getUserId() == null || !isActive(resident.getStatus())) {
                    continue;
                }
                String name = blankToNull(resident.getUserFullName()) != null
                        ? resident.getUserFullName()
                        : resident.getUserId().toString();
                InvoiceRecipientTarget target = new InvoiceRecipientTarget(
                        resident.getUserId(),
                        name,
                        invoiceRef,
                        amount,
                        defaultDueDate);
                unique.putIfAbsent(target.residentId() + "|" + target.invoiceId(), target);
            }
        }

        if (unique.isEmpty()) {
            log.warn("Finance returned invoices for {} but no ACTIVE residents were resolved. Using demo fallback.", period);
            return demoTargets(period, defaultDueDate);
        }

        log.info("Resolved {} invoice notification target(s) from finance+apartment for {}", unique.size(), period);
        return new ArrayList<>(unique.values());
    }

    private List<InvoiceRecipientTarget> demoTargets(YearMonth period, LocalDate dueDate) {
        String suffix = period.toString().replace("-", "");
        return List.of(
                new InvoiceRecipientTarget(RESIDENT_A101, "Resident A101 Owner", "INV-" + suffix + "-A101",
                        new BigDecimal("2450000"), dueDate),
                new InvoiceRecipientTarget(RESIDENT_A102, "Resident A102 Tenant", "INV-" + suffix + "-A102",
                        new BigDecimal("1980000"), dueDate),
                new InvoiceRecipientTarget(RESIDENT_B101, "Resident B101 Owner", "INV-" + suffix + "-B101",
                        new BigDecimal("2125000"), dueDate)
        );
    }

    private boolean isActive(String status) {
        return status != null && "ACTIVE".equalsIgnoreCase(status.trim());
    }

    private boolean isCancelled(String status) {
        return status != null && "CANCELLED".equalsIgnoreCase(status.trim());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
