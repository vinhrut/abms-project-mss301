package com.csms.notification.config;

import com.csms.notification.entity.DeliveryChannel;
import com.csms.notification.entity.InvoiceNotificationDelivery;
import com.csms.notification.entity.InvoiceNotificationDeliveryStatus;
import com.csms.notification.entity.InvoiceNotificationJobRun;
import com.csms.notification.entity.InvoiceNotificationJobStatus;
import com.csms.notification.entity.Notification;
import com.csms.notification.entity.NotificationPriority;
import com.csms.notification.entity.NotificationRecipient;
import com.csms.notification.entity.NotificationStatus;
import com.csms.notification.entity.NotificationType;
import com.csms.notification.repository.InvoiceNotificationDeliveryRepository;
import com.csms.notification.repository.InvoiceNotificationJobRunRepository;
import com.csms.notification.repository.NotificationRecipientRepository;
import com.csms.notification.repository.NotificationRepository;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Seeds demo notifications aligned with auth-service seed users/buildings
 * so History list has data for Admin / Manager A / Resident A101.
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final UUID BUILDING_A = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID BUILDING_B = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ADMIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID MANAGER_A_ID = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID RESIDENT_A101_ID = UUID.fromString("00000000-0000-0000-0000-000000001101");
    private static final UUID RESIDENT_A102_ID = UUID.fromString("00000000-0000-0000-0000-000000001102");

    private static final UUID RESIDENT_B101_ID = UUID.fromString("00000000-0000-0000-0000-000000001201");

    private final NotificationRepository notificationRepository;
    private final NotificationRecipientRepository recipientRepository;
    private final InvoiceNotificationJobRunRepository jobRunRepository;
    private final InvoiceNotificationDeliveryRepository deliveryRepository;

    @Override
    public void run(String... args) {
        LocalDateTime now = LocalDateTime.now();

        List<DemoNotification> demos = List.of(
                new DemoNotification(
                        "DEMO-NOTIF-01",
                        "Bảo trì thang máy Building A",
                        "Thang máy sẽ bảo trì từ 22:00–24:00 ngày mai. Vui lòng dùng cầu thang bộ.",
                        NotificationType.MAINTENANCE,
                        NotificationPriority.HIGH,
                        "ALL",
                        BUILDING_A,
                        NotificationStatus.SENT,
                        now.minusDays(2),
                        now.minusDays(2).plusHours(1)),
                new DemoNotification(
                        "DEMO-NOTIF-02",
                        "Hóa đơn tháng 07/2026 đã phát hành",
                        "Hóa đơn phí quản lý tháng 07 đã sẵn sàng. Vui lòng thanh toán trước ngày 15.",
                        NotificationType.INVOICE,
                        NotificationPriority.NORMAL,
                        "RESIDENT",
                        BUILDING_A,
                        NotificationStatus.SENT,
                        now.minusDays(5),
                        now.minusDays(5).plusMinutes(30)),
                new DemoNotification(
                        "DEMO-NOTIF-03",
                        "Thông báo họp cư dân",
                        "Cuộc họp cư dân Building A diễn ra lúc 19:00 thứ Bảy tại sảnh tầng 1.",
                        NotificationType.ANNOUNCEMENT,
                        NotificationPriority.NORMAL,
                        "RESIDENT",
                        BUILDING_A,
                        NotificationStatus.SENT,
                        now.minusDays(1),
                        now.minusDays(1).plusMinutes(10)),
                new DemoNotification(
                        "DEMO-NOTIF-04",
                        "Hệ thống sẽ bảo trì lúc nửa đêm",
                        "Cổng cư dân tạm ngưng 00:30–01:00 để nâng cấp hệ thống.",
                        NotificationType.SYSTEM,
                        NotificationPriority.URGENT,
                        "ALL",
                        BUILDING_A,
                        NotificationStatus.SENT,
                        now.minusHours(12),
                        now.minusHours(11)),
                new DemoNotification(
                        "DEMO-NOTIF-05",
                        "Nhắc gia hạn hợp đồng thuê",
                        "Hợp đồng căn A-102 sắp hết hạn trong 30 ngày. Liên hệ quản lý để gia hạn.",
                        NotificationType.CONTRACT,
                        NotificationPriority.HIGH,
                        "RESIDENT",
                        BUILDING_A,
                        NotificationStatus.SENT,
                        now.minusDays(3),
                        now.minusDays(3).plusHours(2)),
                new DemoNotification(
                        "DEMO-NOTIF-06",
                        "Lịch trực kỹ thuật tuần này",
                        "Kỹ thuật viên Building A trực từ thứ 2–6, 08:00–17:00.",
                        NotificationType.MAINTENANCE,
                        NotificationPriority.LOW,
                        "STAFF",
                        BUILDING_A,
                        NotificationStatus.SENT,
                        now.minusDays(4),
                        now.minusDays(4).plusHours(1)),
                new DemoNotification(
                        "DEMO-NOTIF-07",
                        "Thông báo Building B — vệ sinh hồ bơi",
                        "Hồ bơi Building B đóng cửa thứ Tư để vệ sinh định kỳ.",
                        NotificationType.ANNOUNCEMENT,
                        NotificationPriority.NORMAL,
                        "ALL",
                        BUILDING_B,
                        NotificationStatus.SENT,
                        now.minusDays(2),
                        now.minusDays(2).plusMinutes(45)),
                new DemoNotification(
                        "DEMO-NOTIF-08",
                        "Draft: Cấm đỗ xe khu vực sảnh",
                        "Nội dung chờ duyệt: từ tuần sau không đỗ xe trước sảnh Building A.",
                        NotificationType.ANNOUNCEMENT,
                        NotificationPriority.NORMAL,
                        "ALL",
                        BUILDING_A,
                        NotificationStatus.PENDING_APPROVAL,
                        now.minusHours(6),
                        null),
                new DemoNotification(
                        "DEMO-NOTIF-09",
                        "Đã lên lịch: Kiểm tra PCCC",
                        "Đội PCCC sẽ kiểm tra tầng hầm vào 09:00 ngày mai.",
                        NotificationType.MAINTENANCE,
                        NotificationPriority.HIGH,
                        "ALL",
                        BUILDING_A,
                        NotificationStatus.SCHEDULED,
                        now.minusHours(3),
                        null),
                new DemoNotification(
                        "DEMO-NOTIF-10",
                        "Gửi email hóa đơn thất bại (demo)",
                        "Một phần cư dân chưa nhận được email hóa đơn — dùng để demo trạng thái FAILED.",
                        NotificationType.INVOICE,
                        NotificationPriority.NORMAL,
                        "RESIDENT",
                        BUILDING_A,
                        NotificationStatus.FAILED,
                        now.minusDays(1),
                        null)
        );

        demos.forEach(this::seedNotification);

        seedRecipientReadState("DEMO-NOTIF-01", RESIDENT_A101_ID, true, now.minusDays(1));
        seedRecipientReadState("DEMO-NOTIF-02", RESIDENT_A101_ID, false, null);
        seedRecipientReadState("DEMO-NOTIF-03", RESIDENT_A101_ID, false, null);
        seedRecipientReadState("DEMO-NOTIF-02", RESIDENT_A102_ID, true, now.minusDays(4));
        seedInvoiceJobMonitor(now);
    }

    private void seedInvoiceJobMonitor(LocalDateTime now) {
        YearMonth period = YearMonth.from(now);
        String key = period.toString();
        if (jobRunRepository.findByBillingPeriod(key).isPresent()) {
            return;
        }

        InvoiceNotificationJobRun run = new InvoiceNotificationJobRun();
        run.setBillingPeriod(key);
        run.setStatus(InvoiceNotificationJobStatus.PARTIAL_SUCCESS);
        run.setStartedAt(now.minusHours(2));
        run.setFinishedAt(now.minusHours(2).plusMinutes(5));
        run.setInvoiceCount(3);
        run.setRecipientCount(3);
        run.setSentCount(5);
        run.setFailedCount(1);
        run.setAttemptNumber(1);
        run.setTriggeredBy(ADMIN_ID);
        run.setErrorMessage("1 delivery channel(s) failed for " + key);
        run = jobRunRepository.save(run);

        String suffix = key.replace("-", "");
        List<InvoiceNotificationDelivery> deliveries = List.of(
                delivery(run, RESIDENT_A101_ID, "Resident A101 Owner", "INV-" + suffix + "-A101",
                        DeliveryChannel.IN_APP, InvoiceNotificationDeliveryStatus.SENT, 1, null),
                delivery(run, RESIDENT_A101_ID, "Resident A101 Owner", "INV-" + suffix + "-A101",
                        DeliveryChannel.EMAIL, InvoiceNotificationDeliveryStatus.SENT, 1, null),
                delivery(run, RESIDENT_A102_ID, "Resident A102 Tenant", "INV-" + suffix + "-A102",
                        DeliveryChannel.IN_APP, InvoiceNotificationDeliveryStatus.SENT, 1, null),
                delivery(run, RESIDENT_A102_ID, "Resident A102 Tenant", "INV-" + suffix + "-A102",
                        DeliveryChannel.EMAIL, InvoiceNotificationDeliveryStatus.FAILED, 1, "SMTP timeout (demo)"),
                delivery(run, RESIDENT_B101_ID, "Resident B101 Owner", "INV-" + suffix + "-B101",
                        DeliveryChannel.IN_APP, InvoiceNotificationDeliveryStatus.SENT, 1, null),
                delivery(run, RESIDENT_B101_ID, "Resident B101 Owner", "INV-" + suffix + "-B101",
                        DeliveryChannel.EMAIL, InvoiceNotificationDeliveryStatus.SENT, 1, null)
        );
        deliveryRepository.saveAll(deliveries);
    }

    private InvoiceNotificationDelivery delivery(
            InvoiceNotificationJobRun run,
            UUID residentId,
            String residentName,
            String invoiceId,
            DeliveryChannel channel,
            InvoiceNotificationDeliveryStatus status,
            int attempt,
            String error) {
        InvoiceNotificationDelivery row = new InvoiceNotificationDelivery();
        row.setJobRun(run);
        row.setResidentId(residentId);
        row.setResidentName(residentName);
        row.setInvoiceId(invoiceId);
        row.setChannel(channel);
        row.setStatus(status);
        row.setAttemptNumber(attempt);
        row.setLastError(error);
        return row;
    }

    private void seedNotification(DemoNotification demo) {
        if (notificationRepository.findBySourceKey(demo.sourceKey()).isPresent()) {
            return;
        }

        Notification notification = new Notification();
        notification.setTitle(demo.title());
        notification.setContent(demo.content());
        notification.setType(demo.type());
        notification.setPriority(demo.priority());
        notification.setRecipientGroup(demo.recipientGroup());
        notification.setBuildingId(demo.buildingId());
        notification.setChannels(EnumSet.of(DeliveryChannel.IN_APP, DeliveryChannel.EMAIL));
        notification.setStatus(demo.status());
        notification.setCreatedBy(MANAGER_A_ID);
        notification.setCreatedAt(demo.createdAt());
        notification.setSourceKey(demo.sourceKey());

        if (demo.status() == NotificationStatus.SENT || demo.status() == NotificationStatus.PARTIAL_FAILED) {
            notification.setApprovedBy(MANAGER_A_ID);
            notification.setApprovedAt(demo.createdAt().plusMinutes(5));
            notification.setSentAt(demo.sentAt());
        }
        if (demo.status() == NotificationStatus.SCHEDULED) {
            notification.setApprovedBy(MANAGER_A_ID);
            notification.setApprovedAt(demo.createdAt().plusMinutes(5));
            notification.setScheduledAt(LocalDateTime.now().plusDays(1).withHour(9).withMinute(0).withSecond(0).withNano(0));
        }
        if (demo.status() == NotificationStatus.FAILED) {
            notification.setApprovedBy(MANAGER_A_ID);
            notification.setApprovedAt(demo.createdAt().plusMinutes(5));
            notification.setFailureReason("Demo seed: SMTP gateway unavailable");
        }
        if (demo.status() == NotificationStatus.PENDING_APPROVAL) {
            notification.setCreatedBy(ADMIN_ID);
        }

        notificationRepository.save(notification);
    }

    private void seedRecipientReadState(String sourceKey, UUID userId, boolean read, LocalDateTime readAt) {
        notificationRepository.findBySourceKey(sourceKey).ifPresent(notification -> {
            if (recipientRepository.existsByNotificationNotificationIdAndUserId(notification.getNotificationId(), userId)) {
                return;
            }
            NotificationRecipient recipient = new NotificationRecipient();
            recipient.setNotification(notification);
            recipient.setUserId(userId);
            recipient.setRead(read);
            recipient.setReadAt(readAt);
            recipientRepository.save(recipient);
        });
    }

    private record DemoNotification(
            String sourceKey,
            String title,
            String content,
            NotificationType type,
            NotificationPriority priority,
            String recipientGroup,
            UUID buildingId,
            NotificationStatus status,
            LocalDateTime createdAt,
            LocalDateTime sentAt
    ) {}
}
