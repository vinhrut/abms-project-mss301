package com.abms.vehicle.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class VehicleLimitRequest {

    @NotNull(message = "Apartment id is required")
    private UUID apartmentId;

    @NotBlank(message = "Vehicle type is required")
    private String vehicleType;

    @Min(value = 0, message = "Max quantity must be greater than or equal to 0")
    private int maxQuantity;
}