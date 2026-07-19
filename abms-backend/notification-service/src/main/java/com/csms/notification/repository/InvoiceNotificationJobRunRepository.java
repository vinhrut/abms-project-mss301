package com.csms.notification.repository;

import com.csms.notification.entity.InvoiceNotificationJobRun;
import com.csms.notification.entity.InvoiceNotificationJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceNotificationJobRunRepository extends JpaRepository<InvoiceNotificationJobRun, UUID> {
    Optional<InvoiceNotificationJobRun> findByBillingPeriod(String billingPeriod);
    boolean existsByBillingPeriodAndStatusIn(String billingPeriod, Iterable<InvoiceNotificationJobStatus> statuses);
    Page<InvoiceNotificationJobRun> findAllByOrderByStartedAtDesc(Pageable pageable);
}
