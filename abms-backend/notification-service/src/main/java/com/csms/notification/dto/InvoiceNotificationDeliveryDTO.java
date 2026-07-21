package com.csms.notification.dto;

import com.csms.notification.entity.DeliveryChannel;
import com.csms.notification.entity.InvoiceNotificationDeliveryStatus;
import lombok.Builder;
import lombok.Value;

import java.util.UUID;

/** Fields aligned with Job Monitor table: Resident, Invoice ID, Channel, Status, Attempt. */
@Value
@Builder
public class InvoiceNotificationDeliveryDTO {
    UUID id;
    UUID residentId;
    String residentName;
    String invoiceId;
    DeliveryChannel channel;
    InvoiceNotificationDeliveryStatus status;
    int attempt;
}
