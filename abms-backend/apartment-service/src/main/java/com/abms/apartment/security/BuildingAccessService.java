package com.abms.apartment.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BuildingAccessService {

    @Value("${jwt.secret}")
    private String secret;

    public void ensureCanViewApartments(String authorizationHeader, UUID requestedBuildingId) {
        Claims claims = getClaims(authorizationHeader);
        String role = claims.get("role", String.class);

        if ("ADMIN".equals(role)) {
            return;
        }

        UUID managerBuildingId = getBuildingId(claims);
        if (!"MANAGER".equals(role) || !requestedBuildingId.equals(managerBuildingId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Manager can only view apartments in own building");
        }
    }

    public void ensureCanViewAllApartments(String authorizationHeader) {
        Claims claims = getClaims(authorizationHeader);
        if (!"ADMIN".equals(claims.get("role", String.class))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only admin can view apartments from all buildings");
        }
    }

    private Claims getClaims(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authorizationHeader.substring(7))
                    .getBody();
        } catch (RuntimeException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid bearer token");
        }
    }

    private UUID getBuildingId(Claims claims) {
        String buildingId = claims.get("buildingId", String.class);
        return buildingId == null ? null : UUID.fromString(buildingId);
    }

    private SecretKey getSigningKey() {
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (IllegalArgumentException exception) {
            return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        }
    }
}
