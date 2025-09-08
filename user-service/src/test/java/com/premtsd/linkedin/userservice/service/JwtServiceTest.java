package com.premtsd.linkedin.userservice.service;

import com.premtsd.linkedin.userservice.entity.Role;
import com.premtsd.linkedin.userservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;
    private final String testSecretKey = "testSecretKeyThatIsLongEnoughForHS256Algorithm";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecretKey", testSecretKey);

        // Create test user
        Role userRole = new Role();
        userRole.setName("USER");

        Role adminRole = new Role();
        adminRole.setName("ADMIN");

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setName("Test User");
        testUser.setRoles(Set.of(userRole, adminRole));
    }

    @Test
    void generateAccessToken_ShouldCreateValidToken_WhenValidUser() {
        // When
        String token = jwtService.generateAccessToken(testUser);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertTrue(token.split("\\.").length == 3); // JWT has 3 parts separated by dots
    }

    @Test
    void generateAccessToken_ShouldIncludeCorrectClaims_WhenValidUser() {
        // When
        String token = jwtService.generateAccessToken(testUser);

        // Then
        SecretKey key = Keys.hmacShaKeyFor(testSecretKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(testUser.getId().toString(), claims.getSubject());
        assertEquals(testUser.getEmail(), claims.get("email"));
        
        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertEquals(2, roles.size());
        assertTrue(roles.contains("USER"));
        assertTrue(roles.contains("ADMIN"));
    }

    @Test
    void generateAccessToken_ShouldSetCorrectTimestamps_WhenValidUser() {
        // Given
        long beforeGeneration = System.currentTimeMillis();

        // When
        String token = jwtService.generateAccessToken(testUser);

        // Then
        long afterGeneration = System.currentTimeMillis();
        
        SecretKey key = Keys.hmacShaKeyFor(testSecretKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        assertNotNull(issuedAt);
        assertNotNull(expiration);
        assertTrue(issuedAt.getTime() >= beforeGeneration - 1000); // Allow 1 second tolerance
        assertTrue(issuedAt.getTime() <= afterGeneration + 1000); // Allow 1 second tolerance
        assertTrue(expiration.after(issuedAt));
        
        // Token should expire in approximately 100 minutes (1000*60*100 ms)
        long expectedExpirationTime = issuedAt.getTime() + (1000L * 60 * 100);
        assertEquals(expectedExpirationTime, expiration.getTime());
    }

    @Test
    void getUserIdFromToken_ShouldReturnCorrectUserId_WhenValidToken() {
        // Given
        String token = jwtService.generateAccessToken(testUser);

        // When
        Long userId = jwtService.getUserIdFromToken(token);

        // Then
        assertEquals(testUser.getId(), userId);
    }

    @Test
    void getUserIdFromToken_ShouldThrowException_WhenInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(Exception.class, () -> jwtService.getUserIdFromToken(invalidToken));
    }

    @Test
    void getUserIdFromToken_ShouldThrowException_WhenExpiredToken() {
        // Given - Create a token that's already expired
        SecretKey key = Keys.hmacShaKeyFor(testSecretKey.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject(testUser.getId().toString())
                .claim("email", testUser.getEmail())
                .issuedAt(new Date(System.currentTimeMillis() - 10000)) // 10 seconds ago
                .expiration(new Date(System.currentTimeMillis() - 5000)) // 5 seconds ago (expired)
                .signWith(key)
                .compact();

        // When & Then
        assertThrows(Exception.class, () -> jwtService.getUserIdFromToken(expiredToken));
    }

    @Test
    void getUserIdFromToken_ShouldThrowException_WhenNullToken() {
        // When & Then
        assertThrows(Exception.class, () -> jwtService.getUserIdFromToken(null));
    }

    @Test
    void getUserIdFromToken_ShouldThrowException_WhenEmptyToken() {
        // When & Then
        assertThrows(Exception.class, () -> jwtService.getUserIdFromToken(""));
    }



    @Test
    void generateAccessToken_ShouldHandleUserWithNoRoles() {
        // Given
        User userWithoutRoles = new User();
        userWithoutRoles.setId(2L);
        userWithoutRoles.setEmail("noroles@example.com");
        userWithoutRoles.setName("No Roles User");
        userWithoutRoles.setRoles(Set.of()); // Empty roles

        // When
        String token = jwtService.generateAccessToken(userWithoutRoles);

        // Then
        assertNotNull(token);
        
        SecretKey key = Keys.hmacShaKeyFor(testSecretKey.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.get("roles");
        assertTrue(roles.isEmpty());
    }
}
