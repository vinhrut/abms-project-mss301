package com.csms.notification.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_recipients", uniqueConstraints = @UniqueConstraint(columnNames = {"notification_id", "user_id"}))
@Getter @Setter @NoArgsConstructor
public class NotificationRecipient {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "receiver_id")
    private UUID receiverId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
