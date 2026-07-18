package com.abms.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }

    public String extractEmail(String token) {
        Object email = extractAllClaims(token).get("email");
        return email != null ? email.toString() : null;
    }

    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    public String extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        return userId != null ? userId.toString() : null;
    }

    public String extractBuildingId(String token) {
        Object buildingId = extractAllClaims(token).get("buildingId");
        return buildingId != null ? buildingId.toString() : null;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
