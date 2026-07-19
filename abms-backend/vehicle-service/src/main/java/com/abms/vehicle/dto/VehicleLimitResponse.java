package com.abms.vehicle.dto;

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
public class VehicleLimitResponse {

    private UUID limitId;
    private UUID apartmentId;
    private UUID buildingId;
    private String vehicleType;
    private int maxQuantity;
    private long approvedVehicleCount;
}