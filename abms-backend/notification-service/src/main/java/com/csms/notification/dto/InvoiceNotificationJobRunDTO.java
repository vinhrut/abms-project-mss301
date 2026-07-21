package com.csms.notification.dto;

import com.csms.notification.entity.InvoiceNotificationJobStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class InvoiceNotificationJobRunDTO {
    UUID id;
    String billingPeriod;
    InvoiceNotificationJobStatus status;
    LocalDateTime startedAt;
    LocalDateTime finishedAt;
    int invoiceCount;
    int recipientCount;
    int sentCount;
    int failedCount;
    int attemptNumber;
    String errorMessage;
    List<InvoiceNotificationDeliveryDTO> deliveries;
}
