package com.premtsd.linkedin.apigateway;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JwtServiceTest {

    private JwtService jwtService;
    private String secret = "thisisaverysecurekeyfortestingpurposesonly!"; // Must be at least 32 bytes
    private String validToken;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        jwtService.jwtSecretKey = secret;
        jwtService.init();

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        validToken = Jwts.builder()
                .subject("user-123")
                .claim("roles", List.of("USER"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 100000))
                .signWith(key)
                .compact();
    }

    @Test
    void testParseTokenReturnsClaims() {
        Claims claims = jwtService.parseToken("Bearer " + validToken);
        assertEquals("user-123", claims.getSubject());
        assertEquals(List.of("USER"), claims.get("roles"));
    }

    @Test
    void testGetUserIdFromToken() {
        String userId = jwtService.getUserIdFromToken("Bearer " + validToken);
        assertEquals("user-123", userId);
    }

    @Test
    void testValidateTokenWithCorrectRole() {
        assertDoesNotThrow(() -> jwtService.validateTokenWithRole("Bearer " + validToken, "USER"));
    }

    @Test
    void testValidateTokenWithWrongRoleThrows() {
        JwtException exception = assertThrows(JwtException.class, () ->
                jwtService.validateTokenWithRole("Bearer " + validToken, "ADMIN"));
        assertEquals("Invalid JWT token or role mismatch.", exception.getMessage());
    }

    @Test
    void testGetClaimReturnsCorrectValue() {
        String subject = jwtService.getClaim("Bearer " + validToken, "sub", String.class);
        assertEquals("user-123", subject);
    }
}
