package com.csms.notification.dto;

import com.csms.notification.entity.DeliveryChannel;
import com.csms.notification.entity.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
public class AnnouncementDTO {
    @NotBlank @Size(max = 255) private String title;
    @NotBlank @Size(max = 2000) private String content;
    private NotificationPriority priority = NotificationPriority.NORMAL;
    @NotBlank private String recipientGroup;
    @NotEmpty private Set<DeliveryChannel> channels;
    private Set<UUID> recipientIds;
    private LocalDateTime scheduledAt;
}
