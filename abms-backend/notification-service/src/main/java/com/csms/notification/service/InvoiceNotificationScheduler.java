package com.csms.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class InvoiceNotificationScheduler {
    private final InvoiceNotificationJobService jobService;

    /** SRS 3.2.5: 08:00 on the first day of each month (Asia/Ho_Chi_Minh). */
    @Scheduled(cron = "${invoice.notification.cron:0 0 8 1 * *}", zone = "${invoice.notification.zone:Asia/Ho_Chi_Minh}")
    public void sendMonthlyInvoiceNotifications() {
        try {
            jobService.run(YearMonth.now(), null, false);
        } catch (Exception exception) {
            log.error("Monthly invoice notification scheduler failed", exception);
        }
    }
}
