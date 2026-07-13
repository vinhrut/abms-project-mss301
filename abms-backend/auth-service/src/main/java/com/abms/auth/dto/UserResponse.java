package com.abms.auth.dto;

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
public class UserResponse {

    private UUID userId;
    private String email;
    private String fullName;
    private String phone;
    private String idCard;
    private String roleName;
    private String status;
    private UUID buildingId;
    private UUID createdBy;
    private UUID lockedBy;
    private LocalDateTime lockedAt;
}