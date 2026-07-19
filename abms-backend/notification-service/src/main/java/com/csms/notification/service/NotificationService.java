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
 NotificationDTO createAnnouncement(AnnouncementDTO dto, UUID actorId, UUID buildingId);
 default NotificationDTO createAnnouncement(AnnouncementDTO dto, UUID actorId) {
  return createAnnouncement(dto, actorId, null);
 }
 NotificationDTO approve(UUID id, UUID actorId, UUID buildingId);
 default NotificationDTO approve(UUID id, UUID actorId) {
  return approve(id, actorId, null);
 }
 NotificationDTO reject(UUID id, UUID actorId, String reason, UUID buildingId);
 default NotificationDTO reject(UUID id, UUID actorId, String reason) {
  return reject(id, actorId, reason, null);
 }
 NotificationDTO cancel(UUID id, UUID actorId, UUID buildingId);
 default NotificationDTO cancel(UUID id, UUID actorId) {
  return cancel(id, actorId, null);
 }
 PageResponse<NotificationDTO> list(UUID userId, String role, UUID buildingId, NotificationType type,
                                     NotificationStatus status, LocalDateTime from, LocalDateTime to,
                                     String recipient, int page, int size);
 default PageResponse<NotificationDTO> list(UUID userId, String role, NotificationType type,
                                             NotificationStatus status, LocalDateTime from, LocalDateTime to,
                                             String recipient, int page, int size) {
  return list(userId, role, null, type, status, from, to, recipient, page, size);
 }
 NotificationDTO detail(UUID id, UUID userId, String role, UUID buildingId);
 default NotificationDTO detail(UUID id, UUID userId, String role) {
  return detail(id, userId, role, null);
 }
 NotificationDTO markRead(UUID id, UUID userId, String role, UUID buildingId);
 default NotificationDTO markRead(UUID id, UUID userId, String role) {
  return markRead(id, userId, role, null);
 }
 void dispatch(Notification notification);
 int dispatchDue();
 int retryFailed(UUID actorId, UUID buildingId);
 default int retryFailed(UUID actorId) {
  return retryFailed(actorId, null);
 }
 NotificationDTO createInvoiceNotification(String title,String content,Set<UUID> recipientIds,UUID actorId);
 default NotificationDTO createInvoiceNotification(String title, String content, Set<UUID> recipientIds,
                                                    UUID actorId, String sourceKey) {
  return createInvoiceNotification(title, content, recipientIds, actorId);
 }
}
