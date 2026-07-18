package com.csms.notification.service;

import com.csms.notification.dto.AnnouncementDTO;
import com.csms.notification.dto.NotificationDTO;
import com.csms.notification.dto.PageResponse;
import com.csms.notification.entity.Notification;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.entity.NotificationType;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public interface NotificationService {
 NotificationDTO createAnnouncement(AnnouncementDTO dto, UUID actorId);
 NotificationDTO approve(UUID id, UUID actorId);
 PageResponse<NotificationDTO> list(UUID userId,String role,NotificationType type,NotificationStatus status,LocalDateTime from,LocalDateTime to,String recipient,int page,int size);
 NotificationDTO detail(UUID id, UUID userId, String role);
 NotificationDTO markRead(UUID id, UUID userId, String role);
 void dispatch(Notification notification);
 int dispatchDue();
 int retryFailed(UUID actorId);
 NotificationDTO createInvoiceNotification(String title,String content,Set<UUID> recipientIds,UUID actorId);
}
