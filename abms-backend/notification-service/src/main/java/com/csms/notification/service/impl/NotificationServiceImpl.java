package com.csms.notification.service.impl;

import com.csms.notification.dto.AnnouncementDTO;
import com.csms.notification.dto.NotificationDTO;
import com.csms.notification.dto.PageResponse;
import com.csms.notification.entity.DeliveryChannel;
import com.csms.notification.entity.Notification;
import com.csms.notification.entity.NotificationPriority;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.entity.NotificationType;
import com.csms.notification.entity.NotificationRecipient;
import com.csms.notification.repository.NotificationRecipientRepository;
import com.csms.notification.repository.NotificationRepository;
import com.csms.notification.service.AuditLogService;
import com.csms.notification.service.EmailGateway;
import com.csms.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {
    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final AuditLogService auditLogService;
    private final EmailGateway emailGateway;

    @Override
    public NotificationDTO createAnnouncement(AnnouncementDTO dto, UUID actorId, UUID buildingId) {
        Notification notification = new Notification();
        notification.setTitle(dto.getTitle().trim());
        notification.setContent(dto.getContent().trim());
        notification.setPriority(dto.getPriority() == null ? NotificationPriority.NORMAL : dto.getPriority());
        notification.setRecipientGroup(dto.getRecipientGroup().trim().toUpperCase());
        notification.setChannels(new HashSet<>(dto.getChannels()));
        notification.setScheduledAt(dto.getScheduledAt());
        notification.setCreatedBy(actorId);
        notification.setBuildingId(buildingId);
        notification.setType(NotificationType.ANNOUNCEMENT);
        notification.setStatus(NotificationStatus.PENDING_APPROVAL);

        notification = notificationRepository.save(notification);
        saveRecipients(notification, dto.getRecipientIds());
        auditLogService.log(actorId, "CREATE_ANNOUNCEMENT", notification.getNotificationId().toString());
        return toDto(notification, actorId, true);
    }

    @Override
    public NotificationDTO approve(UUID id, UUID actorId, UUID buildingId) {
        Notification notification = get(id);
        ensureManagerScope(notification, buildingId);
        if (notification.getStatus() != NotificationStatus.PENDING_APPROVAL) {
            throw bad("Only PENDING_APPROVAL notification can be approved");
        }

        notification.setApprovedBy(actorId);
        notification.setApprovedAt(LocalDateTime.now());

        if (notification.getScheduledAt() != null
                && notification.getScheduledAt().isAfter(LocalDateTime.now())) {
            notification.setStatus(NotificationStatus.SCHEDULED);
        } else {
            notification.setStatus(NotificationStatus.APPROVED);
            dispatch(notification);
        }

        notificationRepository.save(notification);
        auditLogService.log(actorId, "APPROVE_ANNOUNCEMENT", id.toString());
        return toDto(notification, actorId, true);
    }

    @Override
    public NotificationDTO reject(UUID id, UUID actorId, String reason, UUID buildingId) {
        Notification notification = get(id);
        ensureManagerScope(notification, buildingId);
        if (notification.getStatus() != NotificationStatus.PENDING_APPROVAL) {
            throw bad("Only PENDING_APPROVAL notification can be rejected");
        }
        if (reason == null || reason.isBlank()) {
            throw bad("Rejection reason is required");
        }

        notification.setStatus(NotificationStatus.REJECTED);
        notification.setRejectedBy(actorId);
        notification.setRejectedAt(LocalDateTime.now());
        notification.setRejectionReason(reason.trim());
        notificationRepository.save(notification);
        auditLogService.log(actorId, "REJECT_ANNOUNCEMENT", id.toString());
        return toDto(notification, actorId, true);
    }

    @Override
    public NotificationDTO cancel(UUID id, UUID actorId, UUID buildingId) {
        Notification notification = get(id);
        ensureManagerScope(notification, buildingId);
        if (notification.getStatus() != NotificationStatus.PENDING_APPROVAL
                && notification.getStatus() != NotificationStatus.APPROVED
                && notification.getStatus() != NotificationStatus.SCHEDULED) {
            throw bad("Only pending or scheduled notification can be cancelled");
        }

        notification.setStatus(NotificationStatus.CANCELLED);
        notificationRepository.save(notification);
        auditLogService.log(actorId, "CANCEL_NOTIFICATION", id.toString());
        return toDto(notification, actorId, true);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationDTO> list(UUID userId, String role, UUID buildingId, NotificationType type,
                                               NotificationStatus status, LocalDateTime from,
                                               LocalDateTime to, String recipient, int page, int size) {
        boolean manager = isManager(role);
        requireBuildingScope(manager, buildingId);
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Notification> result = notificationRepository.searchVisible(
                userId,
                normalizeRole(role),
                manager,
                buildingId,
                type,
                status,
                from,
                to,
                blankToNull(recipient),
                NotificationStatus.SENT,
                pageable);

        return new PageResponse<>(
                result.map(notification -> toDto(notification, userId, manager)).getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Override
    public NotificationDTO detail(UUID id, UUID userId, String role, UUID buildingId) {
        Notification notification = get(id);
        ensureVisible(notification, userId, role, buildingId);
        auditLogService.log(userId, "VIEW_NOTIFICATION", id.toString());
        return toDto(notification, userId, isManager(role));
    }

    @Override
    public NotificationDTO markRead(UUID id, UUID userId, String role, UUID buildingId) {
        Notification notification = get(id);
        ensureVisible(notification, userId, role, buildingId);

        NotificationRecipient recipient = recipientRepository
                .findByNotificationNotificationIdAndUserId(id, userId)
                .orElseGet(() -> {
                    NotificationRecipient created = new NotificationRecipient();
                    created.setNotification(notification);
                    created.setUserId(userId);
                    return created;
                });

        recipient.setRead(true);
        recipient.setReadAt(LocalDateTime.now());
        recipientRepository.save(recipient);
        auditLogService.log(userId, "MARK_NOTIFICATION_READ", id.toString());
        return toDto(notification, userId, isManager(role));
    }

    @Override
    public void dispatch(Notification notification) {
        if (notification.getStatus() == NotificationStatus.CANCELLED
                || notification.getStatus() == NotificationStatus.REJECTED) {
            return;
        }

        try {
            notification.setStatus(NotificationStatus.PROCESSING);
            notificationRepository.save(notification);

            boolean emailRequested = notification.getChannels().contains(DeliveryChannel.EMAIL);
            boolean inAppRequested = notification.getChannels().contains(DeliveryChannel.IN_APP);
            boolean emailFailed = false;
            String emailFailure = null;

            if (emailRequested) {
                try {
                    emailGateway.send(notification);
                } catch (Exception emailException) {
                    emailFailed = true;
                    emailFailure = truncate(emailException.getMessage(), 1000);
                }
            }

            if (!emailRequested && !inAppRequested) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setFailureReason("At least one delivery channel is required");
            } else if (emailFailed && inAppRequested) {
                notification.setStatus(NotificationStatus.PARTIAL_FAILED);
                notification.setFailureReason(emailFailure);
            } else if (emailFailed) {
                notification.setStatus(NotificationStatus.FAILED);
                notification.setFailureReason(emailFailure);
            } else {
                notification.setStatus(NotificationStatus.SENT);
                notification.setSentAt(LocalDateTime.now());
                notification.setFailureReason(null);
            }
        } catch (Exception exception) {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setFailureReason(truncate(exception.getMessage(), 1000));
        }

        notificationRepository.save(notification);
    }

    @Scheduled(fixedDelayString = "${notification.dispatch-delay-ms:60000}")
    public int dispatchDue() {
        List<Notification> due = notificationRepository.findByStatusInAndScheduledAtLessThanEqual(
                EnumSet.of(NotificationStatus.APPROVED, NotificationStatus.SCHEDULED),
                LocalDateTime.now());
        due.forEach(this::dispatch);
        return due.size();
    }

    @Override
    public int retryFailed(UUID actorId, UUID buildingId) {
        List<Notification> failed = notificationRepository.findByStatusIn(
                EnumSet.of(NotificationStatus.FAILED, NotificationStatus.PARTIAL_FAILED));
        List<Notification> retryable = failed.stream()
                .filter(notification -> buildingId == null || notification.getBuildingId() == null
                        || buildingId.equals(notification.getBuildingId()))
                .toList();
        retryable.forEach(this::dispatch);
        auditLogService.log(actorId, "RETRY_FAILED_NOTIFICATIONS", String.valueOf(retryable.size()));
        return retryable.size();
    }

    @Override
    public NotificationDTO createInvoiceNotification(String title, String content, Set<UUID> ids, UUID actorId) {
        return createInvoiceNotification(title, content, ids, actorId, null);
    }

    @Override
    public NotificationDTO createInvoiceNotification(String title, String content, Set<UUID> ids,
                                                     UUID actorId, String sourceKey) {
        if (sourceKey != null && !sourceKey.isBlank()) {
            Notification existing = notificationRepository.findBySourceKey(sourceKey).orElse(null);
            if (existing != null) {
                if (existing.getStatus() == NotificationStatus.FAILED
                        || existing.getStatus() == NotificationStatus.PARTIAL_FAILED) {
                    dispatch(existing);
                }
                return toDto(existing, actorId, true);
            }
        }
        AnnouncementDTO dto = new AnnouncementDTO();
        dto.setTitle(title);
        dto.setContent(content);
        dto.setPriority(NotificationPriority.HIGH);
        dto.setRecipientGroup("RESIDENT");
        dto.setChannels(Set.of(DeliveryChannel.IN_APP, DeliveryChannel.EMAIL));
        dto.setRecipientIds(ids);

        NotificationDTO created = createAnnouncement(dto, actorId);
        Notification notification = get(created.getId());
        notification.setSourceKey(sourceKey);
        notification.setType(NotificationType.INVOICE);
        notification.setApprovedBy(actorId);
        notification.setApprovedAt(LocalDateTime.now());
        notification.setStatus(NotificationStatus.APPROVED);
        notificationRepository.save(notification);
        dispatch(notification);
        return toDto(notification, actorId, true);
    }

    private void saveRecipients(Notification notification, Set<UUID> ids) {
        if (ids == null) {
            return;
        }
        ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .forEach(userId -> {
                    NotificationRecipient recipient = new NotificationRecipient();
                    recipient.setNotification(notification);
                    recipient.setUserId(userId);
                    recipientRepository.save(recipient);
                });
    }

    private Notification get(UUID id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));
    }

    private void ensureVisible(Notification notification, UUID userId, String role, UUID buildingId) {
        boolean manager = isManager(role);
        requireBuildingScope(manager, buildingId);
        if (buildingId != null && notification.getBuildingId() != null
                && !buildingId.equals(notification.getBuildingId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification is outside your building scope");
        }
        if (manager) {
            return;
        }
        if (notification.getStatus() != NotificationStatus.SENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification is not available yet");
        }
        if ("ALL".equalsIgnoreCase(notification.getRecipientGroup())
                || normalizeRole(role).equalsIgnoreCase(notification.getRecipientGroup())
                || recipientRepository.existsByNotificationNotificationIdAndUserId(notification.getNotificationId(), userId)) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification is outside your access scope");
    }

    private NotificationDTO toDto(Notification notification, UUID userId, boolean manager) {
        boolean read = userId != null
                && recipientRepository.findByNotificationNotificationIdAndUserId(
                        notification.getNotificationId(), userId)
                .map(NotificationRecipient::isRead)
                .orElse(false);

        return NotificationDTO.builder()
                .id(notification.getNotificationId())
                .title(notification.getTitle())
                .content(notification.getContent())
                .type(notification.getType())
                .priority(notification.getPriority())
                .recipientGroup(notification.getRecipientGroup())
                .buildingId(notification.getBuildingId())
                .channels(notification.getChannels())
                .status(notification.getStatus())
                .createdAt(notification.getCreatedAt())
                .scheduledAt(notification.getScheduledAt())
                .createdBy(notification.getCreatedBy())
                .approvedBy(notification.getApprovedBy())
                .approvedAt(notification.getApprovedAt())
                .rejectedBy(notification.getRejectedBy())
                .rejectedAt(notification.getRejectedAt())
                .rejectionReason(notification.getRejectionReason())
                .sentAt(notification.getSentAt())
                .read(read)
                .failureReason(manager ? notification.getFailureReason() : null)
                .build();
    }

    private void ensureManagerScope(Notification notification, UUID buildingId) {
        if (buildingId != null && notification.getBuildingId() != null
                && !buildingId.equals(notification.getBuildingId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Notification is outside your building scope");
        }
    }

    private void requireBuildingScope(boolean manager, UUID buildingId) {
        if (!manager && buildingId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Building scope is required");
        }
    }

    private boolean isManager(String role) {
        String normalized = normalizeRole(role);
        return normalized.equals("ADMIN")
                || normalized.equals("MANAGER")
                || normalized.equals("BUILDING_MANAGER");
    }

    private String normalizeRole(String role) {
        return role == null ? "RESIDENT" : role.replace("ROLE_", "").toUpperCase();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return "Unknown error";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
