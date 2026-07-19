package com.abms.apartment.dto;

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
public class ApartmentResidentResponse {

    private UUID residentId;
    private UUID apartmentId;
    private UUID userId;
    private String relationship;
    private String residenceType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;

    private String userFullName;
    private String userEmail;
    private String userPhone;
    private String userIdCard;
    private String userRoleName;

    // contract info
    private java.util.UUID contractId;
    private String contractType;
    private java.time.LocalDate contractStartDate;
    private java.time.LocalDate contractEndDate;
    private java.math.BigDecimal contractDeposit;
    private String contractStatus;
}
