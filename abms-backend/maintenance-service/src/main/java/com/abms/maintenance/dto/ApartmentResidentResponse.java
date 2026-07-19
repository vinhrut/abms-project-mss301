package com.abms.maintenance.dto;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApartmentResidentResponse {

    private UUID residentId;
    private UUID apartmentId;
    private UUID userId;
    private String relationship;
    private String residenceType;
    private String status;
}
