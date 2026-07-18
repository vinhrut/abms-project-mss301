package com.csms.notification.service.impl;

import com.csms.notification.dto.InvoiceNotificationJobRunDTO;
import com.csms.notification.dto.PageResponse;
import com.csms.notification.entity.InvoiceNotificationJobRun;
import com.csms.notification.entity.InvoiceNotificationJobStatus;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.repository.InvoiceNotificationDataRepository;
import com.csms.notification.repository.InvoiceNotificationJobRunRepository;
import com.csms.notification.service.AuditLogService;
import com.csms.notification.service.InvoiceNotificationJobService;
import com.csms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class InvoiceNotificationJobServiceImpl implements InvoiceNotificationJobService {
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);

    private final InvoiceNotificationJobRunRepository jobRunRepository;
    private final InvoiceNotificationDataRepository dataRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InvoiceNotificationJobRunDTO run(YearMonth period, UUID actorId, boolean forceRetry) {
        if (period == null) period = YearMonth.now();
        UUID effectiveActor = actorId == null ? SYSTEM_ACTOR : actorId;
        String key = period.toString();

        InvoiceNotificationJobRun existing = jobRunRepository.findByBillingPeriod(key).orElse(null);
        if (existing != null && existing.getStatus() == InvoiceNotificationJobStatus.SUCCESS && !forceRetry) {
            return toDto(existing); // idempotent: do not send twice for the same billing period
        }
        if (existing != null && existing.getStatus() == InvoiceNotificationJobStatus.RUNNING) {
            throw new ResponseStatusException(BAD_REQUEST, "Invoice notification job is already running for " + key);
        }

        InvoiceNotificationJobRun run = existing == null ? new InvoiceNotificationJobRun() : existing;
        run.setBillingPeriod(key);
        run.setStatus(InvoiceNotificationJobStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setFinishedAt(null);
        run.setAttemptNumber(Math.max(0, run.getAttemptNumber()) + 1);
        run.setTriggeredBy(effectiveActor);
        run.setErrorMessage(null);
        run.setSentCount(0);
        run.setFailedCount(0);
        run = jobRunRepository.saveAndFlush(run);

        try {
            int invoiceCount = dataRepository.countInvoices(period);
            run.setInvoiceCount(invoiceCount);
            if (invoiceCount == 0) {
                run.setStatus(InvoiceNotificationJobStatus.SKIPPED);
                run.setErrorMessage("Monthly invoices have not been generated for " + key);
                finish(run);
                auditLogService.log(effectiveActor, "SKIP_INVOICE_NOTIFICATION_JOB", key);
                return toDto(run);
            }

            Set<UUID> recipients = dataRepository.findEligibleResidentIds(period);
            run.setRecipientCount(recipients.size());

            String title = "Monthly invoice available - " + String.format("%02d/%d", period.getMonthValue(), period.getYear());
            String content = "Your apartment invoice for " + String.format("%02d/%d", period.getMonthValue(), period.getYear())
                + " is now available. Please review the invoice and complete payment before the due date.";

            var notification = notificationService.createInvoiceNotification(
                title, content, recipients, effectiveActor);

            // createInvoiceNotification handles delivery failures internally and
            // returns FAILED instead of throwing. Do not mark the job successful
            // when the notification itself was not delivered.
            if (notification.getStatus() == NotificationStatus.FAILED) {
                run.setSentCount(0);
                run.setFailedCount(recipients.size());
                run.setStatus(InvoiceNotificationJobStatus.FAILED);
                run.setErrorMessage(truncate(notification.getFailureReason(), 2000));
                finish(run);
                auditLogService.log(effectiveActor, "FAIL_INVOICE_NOTIFICATION_JOB", key);
                return toDto(run);
            }

            run.setSentCount(recipients.size());
            run.setFailedCount(0);
            run.setStatus(InvoiceNotificationJobStatus.SUCCESS);
            finish(run);
            auditLogService.log(effectiveActor, "RUN_INVOICE_NOTIFICATION_JOB", key + ":" + recipients.size());
            return toDto(run);
        } catch (Exception exception) {
            run.setStatus(InvoiceNotificationJobStatus.FAILED);
            run.setFailedCount(Math.max(run.getRecipientCount() - run.getSentCount(), 0));
            run.setErrorMessage(truncate(exception.getMessage(), 2000));
            finish(run);
            auditLogService.log(effectiveActor, "FAIL_INVOICE_NOTIFICATION_JOB", key);
            return toDto(run);
        }
    }

    @Override
    public InvoiceNotificationJobRunDTO retry(UUID jobRunId, UUID actorId) {
        InvoiceNotificationJobRun old = jobRunRepository.findById(jobRunId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Job run not found"));
        if (old.getStatus() != InvoiceNotificationJobStatus.FAILED && old.getStatus() != InvoiceNotificationJobStatus.SKIPPED) {
            throw new ResponseStatusException(BAD_REQUEST, "Only FAILED or SKIPPED jobs can be retried");
        }
        return run(old.period(), actorId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceNotificationJobRunDTO> history(int page, int size) {
        Page<InvoiceNotificationJobRun> result = jobRunRepository.findAllByOrderByStartedAtDesc(
            PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return new PageResponse<>(result.map(this::toDto).getContent(), result.getNumber(), result.getSize(),
            result.getTotalElements(), result.getTotalPages());
    }

    private void finish(InvoiceNotificationJobRun run) {
        run.setFinishedAt(LocalDateTime.now());
        jobRunRepository.save(run);
    }

    private InvoiceNotificationJobRunDTO toDto(InvoiceNotificationJobRun run) {
        return InvoiceNotificationJobRunDTO.builder()
            .id(run.getJobRunId()).billingPeriod(run.getBillingPeriod()).status(run.getStatus())
            .startedAt(run.getStartedAt()).finishedAt(run.getFinishedAt())
            .invoiceCount(run.getInvoiceCount()).recipientCount(run.getRecipientCount())
            .sentCount(run.getSentCount()).failedCount(run.getFailedCount())
            .attemptNumber(run.getAttemptNumber()).errorMessage(run.getErrorMessage()).build();
    }

    private String truncate(String value, int max) {
        if (value == null) return "Unknown error";
        return value.length() <= max ? value : value.substring(0, max);
    }
}
