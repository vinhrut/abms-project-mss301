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
public class VehicleResponse {

    private UUID vehicleId;
    private UUID apartmentId;
    private UUID ownerId;
    private String licensePlate;
    private String type;
    private String brand;
    private String status;
}