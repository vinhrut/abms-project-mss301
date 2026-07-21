package com.csms.notification.client;

import lombok.Data;

import java.util.UUID;

@Data
public class ApartmentResidentClientResponse {
    private UUID residentId;
    private UUID apartmentId;
    private UUID userId;
    private String relationship;
    private String status;
    private String userFullName;
    private String userEmail;
}
