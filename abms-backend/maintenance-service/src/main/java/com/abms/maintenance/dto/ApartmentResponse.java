package com.abms.maintenance.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApartmentResponse {

    private UUID apartmentId;
    private UUID buildingId;
    private String roomNumber;
    private Integer floor;
    private Double area;
    private String status;
}
