package com.abms.auth.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ApartmentResponse {

    private UUID apartmentId;
    private UUID buildingId;
    private String roomNumber;
    private int floor;
    private BigDecimal area;
    private String status;
}