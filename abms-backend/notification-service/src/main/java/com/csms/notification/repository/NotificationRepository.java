package com.csms.notification.repository;

import com.csms.notification.entity.Notification;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.entity.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID>, JpaSpecificationExecutor<Notification> {
 List<Notification> findByStatusAndScheduledAtLessThanEqual(NotificationStatus status, LocalDateTime time);
 List<Notification> findByStatus(NotificationStatus status);
 @Query("select distinct n from Notification n left join NotificationRecipient r on r.notification = n " +
        "where (:admin = true or r.userId = :userId or upper(n.recipientGroup) in ('ALL', upper(:role))) " +
        "and (:type is null or n.type = :type) and (:status is null or n.status = :status) " +
        "and (:fromDate is null or n.createdAt >= :fromDate) and (:toDate is null or n.createdAt <= :toDate) " +
        "and (:recipient is null or lower(n.recipientGroup) like lower(concat('%', :recipient, '%')))" )
 Page<Notification> searchVisible(@Param("userId") UUID userId, @Param("role") String role, @Param("admin") boolean admin,
   @Param("type") NotificationType type, @Param("status") NotificationStatus status,
   @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate,
   @Param("recipient") String recipient, Pageable pageable);
}
