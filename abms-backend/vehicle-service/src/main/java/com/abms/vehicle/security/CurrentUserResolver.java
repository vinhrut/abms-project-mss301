package com.abms.vehicle.security;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CurrentUserResolver {

    public CurrentUser resolve(String userIdHeader, String roleHeader, String buildingIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank() || roleHeader == null || roleHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing authenticated user context");
        }

        UUID userId = UUID.fromString(userIdHeader);
        UUID buildingId = buildingIdHeader == null || buildingIdHeader.isBlank() ? null : UUID.fromString(buildingIdHeader);
        return new CurrentUser(userId, roleHeader.trim().toUpperCase(), buildingId);
    }
}