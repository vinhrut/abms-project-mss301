package com.abms.maintenance.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequestResponse {

    private UUID requestId;
    private String requestCode;
    private UUID apartmentId;
    private UUID senderId;
    private UUID technicianId;
    private String category;
    private String priority;
    private String title;
    private String description;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime assignedAt;
    private LocalDateTime resolvedAt;
}
