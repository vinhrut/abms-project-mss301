package com.csms.notification.repository;

import com.csms.notification.entity.InvoiceNotificationDelivery;
import com.csms.notification.entity.InvoiceNotificationDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InvoiceNotificationDeliveryRepository extends JpaRepository<InvoiceNotificationDelivery, UUID> {
    List<InvoiceNotificationDelivery> findByJobRunJobRunIdOrderByResidentNameAscChannelAsc(UUID jobRunId);

    List<InvoiceNotificationDelivery> findByJobRunJobRunIdAndStatusIn(UUID jobRunId, Collection<InvoiceNotificationDeliveryStatus> statuses);

    void deleteByJobRunJobRunId(UUID jobRunId);

    long countByJobRunJobRunIdAndStatus(UUID jobRunId, InvoiceNotificationDeliveryStatus status);
}
