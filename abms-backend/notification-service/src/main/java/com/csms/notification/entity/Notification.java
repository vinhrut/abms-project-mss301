package com.csms.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter @Setter @NoArgsConstructor
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "notification_id")
    private UUID notificationId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private NotificationType type = NotificationType.ANNOUNCEMENT;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private NotificationPriority priority = NotificationPriority.NORMAL;

    @Column(name = "recipient_group", nullable = false, length = 50)
    private String recipientGroup = "ALL";

    @Column(name = "building_id")
    private UUID buildingId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "notification_channels", joinColumns = @JoinColumn(name = "notification_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false)
    private Set<DeliveryChannel> channels = new HashSet<>();

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private NotificationStatus status = NotificationStatus.PENDING_APPROVAL;

    @Column(name = "created_by")
    private UUID createdBy;
    @Column(name = "approved_by")
    private UUID approvedBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    @Column(name = "rejected_by")
    private UUID rejectedBy;
    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;
    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;
    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "source_key", unique = true, length = 150)
    private String sourceKey;

    @Version
    @Column(name = "version")
    private Long version;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (channels == null || channels.isEmpty()) channels = new HashSet<>(Set.of(DeliveryChannel.IN_APP));
    }
}
