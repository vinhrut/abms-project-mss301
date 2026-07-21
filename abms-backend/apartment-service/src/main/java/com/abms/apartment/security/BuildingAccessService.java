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
        String role = normalizeRole(claims.get("role", String.class));

        if ("ADMIN".equals(role)) {
            return;
        }

        UUID scopedBuildingId = getBuildingId(claims);
        if (!("MANAGER".equals(role) || "STAFF".equals(role)) || !requestedBuildingId.equals(scopedBuildingId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Building scoped users can only view apartments in own building");
        }
    }

    public void ensureCanViewApartmentDetail(String authorizationHeader, UUID apartmentId, UUID apartmentBuildingId, boolean activeResident) {
        Claims claims = getClaims(authorizationHeader);
        String role = normalizeRole(claims.get("role", String.class));

        if ("ADMIN".equals(role)) {
            return;
        }

        if (("MANAGER".equals(role) || "STAFF".equals(role)) && apartmentBuildingId.equals(getBuildingId(claims))) {
            return;
        }

        if ("RESIDENT".equals(role) && activeResident) {
            return;
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to view this apartment");
    }

    public void ensureCanViewAllApartments(String authorizationHeader) {
        Claims claims = getClaims(authorizationHeader);
        if (!"ADMIN".equals(normalizeRole(claims.get("role", String.class)))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Only admin can view apartments from all buildings");
        }
    }

    public void ensureAdmin(String authorizationHeader) {
        Claims claims = getClaims(authorizationHeader);
        if (!"ADMIN".equals(normalizeRole(claims.get("role", String.class)))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admin can manage buildings globally");
        }
    }

    public void ensureCanManageBuilding(String authorizationHeader, UUID buildingId) {
        Claims claims = getClaims(authorizationHeader);
        String role = normalizeRole(claims.get("role", String.class));
        if ("ADMIN".equals(role)) {
            return;
        }
        if ("MANAGER".equals(role) && buildingId != null && buildingId.equals(getBuildingId(claims))) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to manage this building");
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

    private String normalizeRole(String role) {
        if (role == null) {
            return null;
        }
        return role.trim().toUpperCase().replaceFirst("^ROLE_", "");
    }
}
