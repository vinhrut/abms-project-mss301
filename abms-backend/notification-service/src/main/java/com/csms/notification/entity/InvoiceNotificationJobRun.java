package com.csms.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "invoice_notification_job_runs",
       uniqueConstraints = @UniqueConstraint(name = "uk_invoice_notification_period", columnNames = "billing_period"))
@Getter
@Setter
@NoArgsConstructor
public class InvoiceNotificationJobRun {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "job_run_id")
    private UUID jobRunId;

    @Column(name = "billing_period", nullable = false, length = 7)
    private String billingPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceNotificationJobStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "invoice_count", nullable = false)
    private int invoiceCount;

    @Column(name = "recipient_count", nullable = false)
    private int recipientCount;

    @Column(name = "sent_count", nullable = false)
    private int sentCount;

    @Column(name = "failed_count", nullable = false)
    private int failedCount;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    public YearMonth period() {
        return YearMonth.parse(billingPeriod);
    }
}
