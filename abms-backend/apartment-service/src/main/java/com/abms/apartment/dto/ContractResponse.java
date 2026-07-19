package com.abms.apartment.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class ContractResponse {

    private UUID contractId;
    private UUID apartmentId;
    private UUID userId;
    private String contractType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal deposit;
    private String status;

    // enriched fields
    private String userFullName;
    private String userEmail;
    private String userPhone;
    private String apartmentRoomNumber;
}
