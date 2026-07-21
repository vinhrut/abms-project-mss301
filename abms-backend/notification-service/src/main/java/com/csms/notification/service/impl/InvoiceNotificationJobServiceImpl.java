package com.csms.notification.service.impl;

import com.csms.notification.dto.InvoiceNotificationDeliveryDTO;
import com.csms.notification.dto.InvoiceNotificationJobRunDTO;
import com.csms.notification.dto.InvoiceRecipientTarget;
import com.csms.notification.dto.NotificationDTO;
import com.csms.notification.dto.PageResponse;
import com.csms.notification.entity.DeliveryChannel;
import com.csms.notification.entity.InvoiceNotificationDelivery;
import com.csms.notification.entity.InvoiceNotificationDeliveryStatus;
import com.csms.notification.entity.InvoiceNotificationJobRun;
import com.csms.notification.entity.InvoiceNotificationJobStatus;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.repository.InvoiceNotificationDataRepository;
import com.csms.notification.repository.InvoiceNotificationDeliveryRepository;
import com.csms.notification.repository.InvoiceNotificationJobRunRepository;
import com.csms.notification.service.AuditLogService;
import com.csms.notification.service.InvoiceNotificationJobService;
import com.csms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceNotificationJobServiceImpl implements InvoiceNotificationJobService {
    private static final UUID SYSTEM_ACTOR = new UUID(0L, 0L);
    private static final int MAX_ATTEMPTS = 3;
    private static final String PAYMENT_LINK = "/app/billing/invoices";

    private final InvoiceNotificationJobRunRepository jobRunRepository;
    private final InvoiceNotificationDeliveryRepository deliveryRepository;
    private final InvoiceNotificationDataRepository dataRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InvoiceNotificationJobRunDTO run(YearMonth period, UUID actorId, boolean forceRetry) {
        if (period == null) {
            period = YearMonth.now();
        }
        UUID effectiveActor = actorId == null ? SYSTEM_ACTOR : actorId;
        String key = period.toString();

        InvoiceNotificationJobRun existing = jobRunRepository.findByBillingPeriod(key).orElse(null);
        if (existing != null && existing.getStatus() == InvoiceNotificationJobStatus.SUCCESS && !forceRetry) {
            return toDto(existing);
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

        if (run.getJobRunId() != null) {
            deliveryRepository.deleteByJobRunJobRunId(run.getJobRunId());
        }

        try {
            List<InvoiceRecipientTarget> targets = dataRepository.findEligibleTargets(period);
            run.setInvoiceCount((int) targets.stream().map(InvoiceRecipientTarget::invoiceId).distinct().count());
            run.setRecipientCount((int) targets.stream().map(InvoiceRecipientTarget::residentId).distinct().count());

            if (targets.isEmpty()) {
                run.setStatus(InvoiceNotificationJobStatus.SKIPPED);
                run.setErrorMessage("No eligible invoice recipients found for " + key);
                finish(run);
                auditLogService.log(effectiveActor, "SKIP_INVOICE_NOTIFICATION_JOB", key + ":NO_RECIPIENTS");
                return toDto(run);
            }

            List<InvoiceNotificationDelivery> deliveries = new ArrayList<>();
            int sent = 0;
            int failed = 0;

            for (InvoiceRecipientTarget target : targets) {
                String title = "Monthly invoice available - "
                        + String.format("%02d/%d", period.getMonthValue(), period.getYear());
                String content = buildMsgNotif001(target, period);
                String sourceKey = "INVOICE_NOTIFICATION:" + key + ":" + target.residentId();

                NotificationStatus notifyStatus;
                String failureReason = null;
                try {
                    NotificationDTO notification = notificationService.createInvoiceNotification(
                            title, content, Set.of(target.residentId()), effectiveActor, sourceKey);
                    notifyStatus = notification.getStatus();
                    failureReason = notification.getFailureReason();
                } catch (Exception exception) {
                    notifyStatus = NotificationStatus.FAILED;
                    failureReason = exception.getMessage();
                }

                boolean inAppOk = notifyStatus != NotificationStatus.FAILED;
                boolean emailOk = notifyStatus == NotificationStatus.SENT;

                InvoiceNotificationDelivery inApp = buildDelivery(
                        run, target, DeliveryChannel.IN_APP, run.getAttemptNumber(),
                        inAppOk, inAppOk ? null : failureReason);
                InvoiceNotificationDelivery email = buildDelivery(
                        run, target, DeliveryChannel.EMAIL, run.getAttemptNumber(),
                        emailOk, emailOk ? null : (failureReason == null ? "Email delivery failed" : failureReason));

                deliveries.add(inApp);
                deliveries.add(email);
                if (inApp.getStatus() == InvoiceNotificationDeliveryStatus.SENT) {
                    sent++;
                } else {
                    failed++;
                }
                if (email.getStatus() == InvoiceNotificationDeliveryStatus.SENT) {
                    sent++;
                } else {
                    failed++;
                }
            }

            deliveryRepository.saveAll(deliveries);
            run.setSentCount(sent);
            run.setFailedCount(failed);
            applyJobStatus(run, sent, failed, key);
            finish(run);
            auditLogService.log(effectiveActor, "RUN_INVOICE_NOTIFICATION_JOB",
                    key + ":sent=" + sent + ":failed=" + failed);
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public InvoiceNotificationJobRunDTO retry(UUID jobRunId, UUID actorId) {
        InvoiceNotificationJobRun old = jobRunRepository.findById(jobRunId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Job run not found"));

        List<InvoiceNotificationDelivery> failed = deliveryRepository.findByJobRunJobRunIdAndStatusIn(
                jobRunId,
                EnumSet.of(InvoiceNotificationDeliveryStatus.FAILED, InvoiceNotificationDeliveryStatus.RETRYING));

        if (!failed.isEmpty()) {
            boolean allMaxed = failed.stream().allMatch(d -> d.getAttemptNumber() >= MAX_ATTEMPTS);
            if (allMaxed) {
                throw new ResponseStatusException(BAD_REQUEST,
                        "All failed deliveries already reached max attempts (" + MAX_ATTEMPTS + ")");
            }
            return retryFailedDeliveries(old, failed, actorId == null ? SYSTEM_ACTOR : actorId);
        }

        if (old.getStatus() != InvoiceNotificationJobStatus.FAILED
                && old.getStatus() != InvoiceNotificationJobStatus.SKIPPED
                && old.getStatus() != InvoiceNotificationJobStatus.PARTIAL_SUCCESS) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "Only FAILED, PARTIAL_SUCCESS or SKIPPED jobs can be retried");
        }
        return run(old.period(), actorId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<InvoiceNotificationJobRunDTO> history(int page, int size) {
        Page<InvoiceNotificationJobRun> result = jobRunRepository.findAllByOrderByStartedAtDesc(
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        return new PageResponse<>(
                result.map(this::toDto).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private InvoiceNotificationJobRunDTO retryFailedDeliveries(
            InvoiceNotificationJobRun run,
            List<InvoiceNotificationDelivery> failed,
            UUID actorId) {

        run.setStatus(InvoiceNotificationJobStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        run.setFinishedAt(null);
        run.setAttemptNumber(Math.max(0, run.getAttemptNumber()) + 1);
        run.setTriggeredBy(actorId);
        run.setErrorMessage(null);
        jobRunRepository.saveAndFlush(run);

        int recovered = 0;
        int stillFailed = 0;
        YearMonth period = run.period();

        for (InvoiceNotificationDelivery delivery : failed) {
            if (delivery.getAttemptNumber() >= MAX_ATTEMPTS) {
                stillFailed++;
                continue;
            }
            delivery.setStatus(InvoiceNotificationDeliveryStatus.RETRYING);
            delivery.setAttemptNumber(delivery.getAttemptNumber() + 1);

            InvoiceRecipientTarget target = new InvoiceRecipientTarget(
                    delivery.getResidentId(),
                    delivery.getResidentName(),
                    delivery.getInvoiceId(),
                    BigDecimal.ZERO,
                    period.plusMonths(1).atDay(15));
            String title = "Monthly invoice available - "
                    + String.format("%02d/%d", period.getMonthValue(), period.getYear());
            String content = buildMsgNotif001(target, period);
            String sourceKey = "INVOICE_NOTIFICATION:" + run.getBillingPeriod() + ":"
                    + delivery.getResidentId() + ":RETRY:" + delivery.getAttemptNumber();

            try {
                NotificationDTO notification = notificationService.createInvoiceNotification(
                        title, content, Set.of(delivery.getResidentId()), actorId, sourceKey);
                boolean ok = delivery.getChannel() == DeliveryChannel.IN_APP
                        ? notification.getStatus() != NotificationStatus.FAILED
                        : notification.getStatus() == NotificationStatus.SENT;
                if (ok) {
                    delivery.setStatus(InvoiceNotificationDeliveryStatus.SENT);
                    delivery.setLastError(null);
                    recovered++;
                } else {
                    delivery.setStatus(InvoiceNotificationDeliveryStatus.FAILED);
                    delivery.setLastError(truncate(notification.getFailureReason(), 1000));
                    stillFailed++;
                }
            } catch (Exception exception) {
                delivery.setStatus(InvoiceNotificationDeliveryStatus.FAILED);
                delivery.setLastError(truncate(exception.getMessage(), 1000));
                stillFailed++;
            }
        }

        deliveryRepository.saveAll(failed);

        long sent = deliveryRepository.countByJobRunJobRunIdAndStatus(
                run.getJobRunId(), InvoiceNotificationDeliveryStatus.SENT);
        long failedCount = deliveryRepository.countByJobRunJobRunIdAndStatus(
                run.getJobRunId(), InvoiceNotificationDeliveryStatus.FAILED);
        run.setSentCount((int) sent);
        run.setFailedCount((int) failedCount);
        applyJobStatus(run, (int) sent, (int) failedCount, run.getBillingPeriod());
        finish(run);
        auditLogService.log(actorId, "RETRY_INVOICE_NOTIFICATION_JOB",
                run.getBillingPeriod() + ":recovered=" + recovered + ":failed=" + stillFailed);
        return toDto(run);
    }

    private InvoiceNotificationDelivery buildDelivery(
            InvoiceNotificationJobRun run,
            InvoiceRecipientTarget target,
            DeliveryChannel channel,
            int attempt,
            boolean success,
            String error) {

        InvoiceNotificationDelivery delivery = new InvoiceNotificationDelivery();
        delivery.setJobRun(run);
        delivery.setResidentId(target.residentId());
        delivery.setResidentName(target.residentName() == null
                ? target.residentId().toString()
                : target.residentName());
        delivery.setInvoiceId(target.invoiceId());
        delivery.setChannel(channel);
        delivery.setAttemptNumber(attempt);
        delivery.setStatus(success
                ? InvoiceNotificationDeliveryStatus.SENT
                : InvoiceNotificationDeliveryStatus.FAILED);
        delivery.setLastError(success ? null : truncate(error, 1000));
        return delivery;
    }

    private void applyJobStatus(InvoiceNotificationJobRun run, int sent, int failed, String key) {
        if (failed == 0) {
            run.setStatus(InvoiceNotificationJobStatus.SUCCESS);
            run.setErrorMessage(null);
        } else if (sent == 0) {
            run.setStatus(InvoiceNotificationJobStatus.FAILED);
            run.setErrorMessage("All invoice notification deliveries failed for " + key);
        } else {
            run.setStatus(InvoiceNotificationJobStatus.PARTIAL_SUCCESS);
            run.setErrorMessage(failed + " delivery channel(s) failed for " + key);
        }
    }

    /** MSG-NOTIF-001: amount, due date, payment link. */
    private String buildMsgNotif001(InvoiceRecipientTarget target, YearMonth period) {
        NumberFormat money = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        String amount = money.format(target.amount() == null ? BigDecimal.ZERO : target.amount());
        String due = (target.dueDate() == null ? period.plusMonths(1).atDay(15) : target.dueDate())
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        return "MSG-NOTIF-001\n"
                + "Your apartment invoice for " + String.format("%02d/%d", period.getMonthValue(), period.getYear())
                + " is now available.\n"
                + "Invoice: " + target.invoiceId() + "\n"
                + "Amount: " + amount + "\n"
                + "Due date: " + due + "\n"
                + "Payment link: " + PAYMENT_LINK;
    }

    private void finish(InvoiceNotificationJobRun run) {
        run.setFinishedAt(LocalDateTime.now());
        jobRunRepository.save(run);
    }

    private InvoiceNotificationJobRunDTO toDto(InvoiceNotificationJobRun run) {
        List<InvoiceNotificationDeliveryDTO> deliveries = deliveryRepository
                .findByJobRunJobRunIdOrderByResidentNameAscChannelAsc(run.getJobRunId())
                .stream()
                .map(this::toDeliveryDto)
                .collect(Collectors.toList());

        return InvoiceNotificationJobRunDTO.builder()
                .id(run.getJobRunId())
                .billingPeriod(run.getBillingPeriod())
                .status(run.getStatus())
                .startedAt(run.getStartedAt())
                .finishedAt(run.getFinishedAt())
                .invoiceCount(run.getInvoiceCount())
                .recipientCount(run.getRecipientCount())
                .sentCount(run.getSentCount())
                .failedCount(run.getFailedCount())
                .attemptNumber(run.getAttemptNumber())
                .errorMessage(run.getErrorMessage())
                .deliveries(deliveries)
                .build();
    }

    private InvoiceNotificationDeliveryDTO toDeliveryDto(InvoiceNotificationDelivery delivery) {
        return InvoiceNotificationDeliveryDTO.builder()
                .id(delivery.getDeliveryId())
                .residentId(delivery.getResidentId())
                .residentName(delivery.getResidentName())
                .invoiceId(delivery.getInvoiceId())
                .channel(delivery.getChannel())
                .status(delivery.getStatus())
                .attempt(delivery.getAttemptNumber())
                .build();
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return "Unknown error";
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
