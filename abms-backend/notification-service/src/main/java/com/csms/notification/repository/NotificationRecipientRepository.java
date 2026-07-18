package com.csms.notification.repository;

import com.csms.notification.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, UUID> {
 Optional<NotificationRecipient> findByNotificationNotificationIdAndUserId(UUID notificationId, UUID userId);
 boolean existsByNotificationNotificationIdAndUserId(UUID notificationId, UUID userId);
}
