package com.abms.apartment.dto;

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
public class ResidentRegistrationRequest {

    @NotNull(message = "User id is required")
    private UUID userId;

    @NotNull(message = "Apartment id is required")
    private UUID apartmentId;

    private String relationship;

    private String residenceType;
}