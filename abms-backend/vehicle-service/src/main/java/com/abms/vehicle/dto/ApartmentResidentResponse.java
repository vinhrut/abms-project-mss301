package com.abms.vehicle.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
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
}