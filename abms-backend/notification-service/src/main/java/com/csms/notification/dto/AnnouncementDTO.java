package com.csms.notification.dto;

import com.csms.notification.entity.DeliveryChannel;
import com.csms.notification.entity.NotificationPriority;
import jakarta.validation.constraints.FutureOrPresent;
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
    @NotBlank @Size(max = 50) private String recipientGroup;
    @NotEmpty private Set<DeliveryChannel> channels;
    @Size(max = 500) private Set<UUID> recipientIds;
    @FutureOrPresent private LocalDateTime scheduledAt;
}
