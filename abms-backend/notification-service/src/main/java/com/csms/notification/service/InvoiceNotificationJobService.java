package com.csms.notification.service;

import com.csms.notification.dto.InvoiceNotificationJobRunDTO;
import com.csms.notification.dto.PageResponse;

import java.time.YearMonth;
import java.util.UUID;

public interface InvoiceNotificationJobService {
    InvoiceNotificationJobRunDTO run(YearMonth period, UUID actorId, boolean forceRetry);
    InvoiceNotificationJobRunDTO retry(UUID jobRunId, UUID actorId);
    PageResponse<InvoiceNotificationJobRunDTO> history(int page, int size);
}
