package com.premtsd.linkedin.e2e.journey;

import com.premtsd.linkedin.e2e.config.E2ETestConfig;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.is;

@DisplayName("Journey 5: Security and Authorization")
class SecurityJourneyTest extends E2ETestConfig {

    @Test
    @DisplayName("Should return 401 with no token on protected endpoint")
    void shouldReturn401WithNoToken() {
        given()
        .when()
            .get(POSTS_BASE + "/core/1")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Should return 401 with invalid token")
    void shouldReturn401WithInvalidToken() {
        given()
            .header("Authorization", "Bearer invalid.token.here")
        .when()
            .get(CONNECTIONS_BASE + "/first-degree")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Should return 401 with expired token")
    void shouldReturn401WithExpiredToken() {
        String expiredToken =
            "eyJhbGciOiJIUzI1NiJ9." +
            "eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiZW1haWwiOiJ0ZXN0QHRlc3QuY29tIiwi" +
            "aWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2MDAwMDAwMDB9." +
            "invalidSignature";

        given()
            .header("Authorization", "Bearer " + expiredToken)
        .when()
            .get(NOTIFICATIONS_BASE)
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Should allow unauthenticated access to signup")
    void shouldAllowUnauthenticatedSignup() {
        // Auth endpoints don't require AuthenticationFilter
        given()
            .contentType("application/json")
            .body("""
                {
                    "email": "test@test.com",
                    "password": "Test@12345",
                    "name": "Test",
                    "roles": ["USER"]
                }
                """)
        .when()
            .post(AUTH_BASE + "/signup")
        .then()
            .statusCode(anyOf(is(201), is(400)));
    }

    @Test
    @DisplayName("Should allow unauthenticated access to login")
    void shouldAllowUnauthenticatedLogin() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "email": "nonexistent@test.com",
                    "password": "Test@12345"
                }
                """)
        .when()
            .post(AUTH_BASE + "/login")
        .then()
            .statusCode(anyOf(is(200), is(404)));
    }

    @Test
    @DisplayName("Should return 401 for connections without auth")
    void shouldReturn401ForConnectionsWithoutAuth() {
        given()
        .when()
            .post(CONNECTIONS_BASE + "/request/1")
        .then()
            .statusCode(401);
    }
}
