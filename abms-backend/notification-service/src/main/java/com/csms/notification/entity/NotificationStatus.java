package com.csms.notification.entity;

public enum NotificationStatus {
    PENDING_APPROVAL,
    APPROVED,
    SCHEDULED,
    PROCESSING,
    SENT,
    PARTIAL_FAILED,
    FAILED,
    REJECTED,
    CANCELLED,
    /** Legacy value retained for rows created before the approval state model. */
    PENDING
}
