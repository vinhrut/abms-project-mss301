package com.csms.notification.repository;

import com.csms.notification.entity.Notification;
import com.csms.notification.entity.NotificationRecipient;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.entity.NotificationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * PostgreSQL-safe filters. Avoids JPQL patterns like
 * {@code (:param is null or lower(x) like lower(concat('%', :param, '%')))}
 * which Hibernate translates with {@code ||} and PostgreSQL then types as bytea.
 */
public final class NotificationSpecifications {

    private NotificationSpecifications() {}

    public static Specification<Notification> searchVisible(
            UUID userId,
            String role,
            boolean admin,
            UUID buildingId,
            NotificationType type,
            NotificationStatus status,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String recipientPattern,
            NotificationStatus sentStatus) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            String normalizedRole = role == null ? "RESIDENT" : role.toUpperCase(Locale.ROOT);

            if (buildingId != null) {
                predicates.add(cb.or(
                        cb.equal(root.get("buildingId"), buildingId),
                        cb.and(
                                cb.isNull(root.get("buildingId")),
                                cb.or(
                                        cb.equal(cb.upper(root.get("recipientGroup")), "ALL"),
                                        cb.exists(recipientExists(query, cb, root, userId))
                                )
                        )
                ));
            }

            if (!admin) {
                predicates.add(cb.equal(root.get("status"), sentStatus));
                predicates.add(cb.or(
                        cb.exists(recipientExists(query, cb, root, userId)),
                        cb.equal(cb.upper(root.get("recipientGroup")), "ALL"),
                        cb.equal(cb.upper(root.get("recipientGroup")), normalizedRole)
                ));
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }
            if (recipientPattern != null && !"%".equals(recipientPattern)) {
                predicates.add(cb.like(cb.lower(root.get("recipientGroup")), recipientPattern));
            }

            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Subquery<Integer> recipientExists(
            CriteriaQuery<?> query,
            CriteriaBuilder cb,
            Root<Notification> notificationRoot,
            UUID userId) {

        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<NotificationRecipient> recipientRoot = subquery.from(NotificationRecipient.class);
        subquery.select(cb.literal(1));
        subquery.where(
                cb.equal(recipientRoot.get("notification"), notificationRoot),
                cb.equal(recipientRoot.get("userId"), userId)
        );
        return subquery;
    }
}
