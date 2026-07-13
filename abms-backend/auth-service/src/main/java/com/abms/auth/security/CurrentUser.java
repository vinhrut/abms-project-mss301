package com.abms.auth.security;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CurrentUser {

    private final UUID userId;
    private final String roleName;
    private final UUID buildingId;
}