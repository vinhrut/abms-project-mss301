package com.abms.auth.dto;

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
public class ApartmentResidentResponse {

    private String residentId;
    private String apartmentId;
    private String userId;
    private String relationship;
    private String residenceType;
    private String status;
}