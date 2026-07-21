package com.csms.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "invoice_notification_deliveries",
        indexes = {
                @Index(name = "idx_invoice_notif_delivery_job", columnList = "job_run_id"),
                @Index(name = "idx_invoice_notif_delivery_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
public class InvoiceNotificationDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "delivery_id")
    private UUID deliveryId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_run_id", nullable = false)
    private InvoiceNotificationJobRun jobRun;

    @Column(name = "resident_id", nullable = false)
    private UUID residentId;

    @Column(name = "resident_name", length = 150)
    private String residentName;

    @Column(name = "invoice_id", nullable = false, length = 64)
    private String invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryChannel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvoiceNotificationDeliveryStatus status;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber = 1;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
        if (attemptNumber < 1) {
            attemptNumber = 1;
        }
    }
}
