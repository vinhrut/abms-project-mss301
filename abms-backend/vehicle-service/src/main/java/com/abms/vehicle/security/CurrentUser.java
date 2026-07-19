package com.abms.vehicle.security;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CurrentUser {

    private UUID userId;
    private String roleName;
    private UUID buildingId;
}