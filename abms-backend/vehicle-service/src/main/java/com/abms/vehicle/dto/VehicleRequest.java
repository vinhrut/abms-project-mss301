package com.abms.vehicle.dto;

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
public class VehicleRequest {

    @NotNull(message = "Apartment id is required")
    private UUID apartmentId;

    private UUID ownerId;

    @NotBlank(message = "License plate is required")
    private String licensePlate;

    @NotBlank(message = "Type is required")
    private String type;

    private String brand;
}