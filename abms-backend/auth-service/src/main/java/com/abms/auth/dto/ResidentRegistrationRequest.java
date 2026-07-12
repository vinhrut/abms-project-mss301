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
public class ResidentRegistrationRequest {

    private String userId;
    private String apartmentId;
    private String relationship;
    private String residenceType;
}