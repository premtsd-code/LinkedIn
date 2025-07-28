package com.premtsd.linkedin.apigateway;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
public class JwtService {

    @Value("${jwt.secretKey}")
    String jwtSecretKey;

    private SecretKey secretKey;

    @PostConstruct
    void init() {
        log.info("Initializing JWT Service...");
        // Ensure key length is valid for HMAC
        byte[] keyBytes = jwtSecretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            log.error("JWT secret key length is insufficient: {} bytes", keyBytes.length);
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 bytes).");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT Service initialized successfully");
    }

    public String getUserIdFromToken(String token) {
        log.debug("Extracting user ID from JWT token");
        try {
            Claims claims = parseToken(token);
            String userId = claims.getSubject();
            log.debug("Successfully extracted user ID: {}", userId);
            return userId;
        } catch (JwtException e) {
            log.error("Failed to parse JWT: {}", e.getMessage());
            throw new JwtException("Invalid JWT token");
        }
    }

    public void validateTokenWithRole(String token, String expectedRole) {
        log.debug("Validating token with required role: {}", expectedRole);
        try {
            Claims claims = parseToken(token);
            List<String> roles = claims.get("roles", List.class);
            log.debug("User roles from token: {}", roles);

            if (roles == null || !roles.contains(expectedRole)) {
                log.warn("Token does not contain required role: {}. User roles: {}", expectedRole, roles);
                throw new JwtException("User is not authorized.");
            }
            log.debug("Role validation successful for role: {}", expectedRole);
        } catch (JwtException e) {
            log.error("JWT validation failed: {}", e.getMessage());
            throw new JwtException("Invalid JWT token or role mismatch.");
        }
    }

    public Claims parseToken(String token) {
        String cleanToken = token.replaceFirst("(?i)^Bearer ", "").trim();
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(cleanToken)
                .getPayload();
    }

    public <T> T getClaim(String token, String claimKey, Class<T> clazz) {
        try {
            Claims claims = parseToken(token);
            return claims.get(claimKey, clazz);
        } catch (JwtException e) {
            log.error("Error getting claim [{}]: {}", claimKey, e.getMessage());
            throw new JwtException("Failed to extract claim from token");
        }
    }
}
