package com.abms.auth.security;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    public CurrentUser parseCurrentUser(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank() || !jwtUtil.isTokenValid(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }

        UUID userId = jwtUtil.extractUserId(token);
        String roleName = jwtUtil.extractRole(token);
        UUID buildingId = jwtUtil.extractBuildingId(token);

        if (userId == null || roleName == null || roleName.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token payload is invalid");
        }

        return new CurrentUser(userId, roleName, buildingId);
    }
}