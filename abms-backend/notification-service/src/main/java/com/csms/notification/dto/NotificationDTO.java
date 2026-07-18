package com.csms.notification.dto;

import com.csms.notification.entity.NotificationPriority;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.entity.NotificationType;
import com.csms.notification.entity.DeliveryChannel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data @Builder
public class NotificationDTO {
 private UUID id; private String title; private String content; private NotificationType type;
 private NotificationPriority priority; private String recipientGroup; private UUID buildingId; private Set<DeliveryChannel> channels;
 private NotificationStatus status; private LocalDateTime createdAt; private LocalDateTime scheduledAt;
 private LocalDateTime approvedAt; private UUID createdBy; private UUID approvedBy;
 private LocalDateTime rejectedAt; private UUID rejectedBy; private String rejectionReason;
 private LocalDateTime sentAt; private boolean read; private String failureReason;
}
