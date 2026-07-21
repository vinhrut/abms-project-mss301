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
public class MaintenanceHistoryResponse {

    private UUID historyId;
    private UUID requestId;
    private String fromStatus;
    private String toStatus;
    private UUID changedBy;
    private String note;
    private LocalDateTime changedAt;
}
