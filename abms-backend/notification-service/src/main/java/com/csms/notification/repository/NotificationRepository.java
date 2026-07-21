package com.csms.notification.repository;

import com.csms.notification.entity.Notification;
import com.csms.notification.entity.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {
    List<Notification> findByStatusInAndScheduledAtLessThanEqual(Collection<NotificationStatus> statuses, LocalDateTime time);

    List<Notification> findByStatusIn(Collection<NotificationStatus> statuses);

    Optional<Notification> findBySourceKey(String sourceKey);
}
