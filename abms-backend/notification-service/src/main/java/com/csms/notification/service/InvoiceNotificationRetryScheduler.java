package com.csms.notification.service;

import com.csms.notification.entity.InvoiceNotificationDelivery;
import com.csms.notification.entity.InvoiceNotificationDeliveryStatus;
import com.csms.notification.entity.InvoiceNotificationJobRun;
import com.csms.notification.entity.InvoiceNotificationJobStatus;
import com.csms.notification.repository.InvoiceNotificationDeliveryRepository;
import com.csms.notification.repository.InvoiceNotificationJobRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

/**
 * A1: retry email/in-app failures after 30 minutes, up to 3 attempts.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceNotificationRetryScheduler {
    private static final int MAX_ATTEMPTS = 3;

    private final InvoiceNotificationJobRunRepository jobRunRepository;
    private final InvoiceNotificationDeliveryRepository deliveryRepository;
    private final InvoiceNotificationJobService jobService;

    @Scheduled(fixedDelayString = "${invoice.notification.retry-delay-ms:1800000}")
    public void retryFailedDeliveries() {
        List<InvoiceNotificationJobRun> recent = jobRunRepository
                .findAllByOrderByStartedAtDesc(PageRequest.of(0, 20))
                .getContent();

        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        for (InvoiceNotificationJobRun run : recent) {
            if (run.getStatus() != InvoiceNotificationJobStatus.FAILED
                    && run.getStatus() != InvoiceNotificationJobStatus.PARTIAL_SUCCESS) {
                continue;
            }
            if (run.getFinishedAt() != null && run.getFinishedAt().isAfter(threshold)) {
                continue;
            }

            List<InvoiceNotificationDelivery> failed = deliveryRepository.findByJobRunJobRunIdAndStatusIn(
                    run.getJobRunId(),
                    EnumSet.of(InvoiceNotificationDeliveryStatus.FAILED));
            boolean retryable = failed.stream().anyMatch(d -> d.getAttemptNumber() < MAX_ATTEMPTS);
            if (!retryable) {
                continue;
            }

            try {
                log.info("Auto-retrying invoice notification job {}", run.getBillingPeriod());
                jobService.retry(run.getJobRunId(), null);
            } catch (Exception exception) {
                log.warn("Auto-retry skipped for {}: {}", run.getBillingPeriod(), exception.getMessage());
            }
        }
    }
}
