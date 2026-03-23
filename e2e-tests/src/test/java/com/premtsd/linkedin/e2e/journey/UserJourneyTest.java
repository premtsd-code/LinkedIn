package com.premtsd.linkedin.e2e.journey;

import com.premtsd.linkedin.e2e.config.E2ETestConfig;
import com.premtsd.linkedin.e2e.helper.AuthHelper;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Journey 1: User Registration and Login")
class UserJourneyTest extends E2ETestConfig {

    private static String token;
    private static final String EMAIL = AuthHelper.uniqueEmail();
    private static final String PASSWORD = "Test@12345";
    private static final String NAME = "E2E Test User";

    @Test
    @Order(1)
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUser() {
        // Given / When / Then — signup returns UserDto (no token)
        given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "name": "%s",
                    "email": "%s",
                    "password": "%s",
                    "roles": ["USER"]
                }
                """, NAME, EMAIL, PASSWORD))
        .when()
            .post(AUTH_BASE + "/signup")
        .then()
            .statusCode(201)
            .body("email", equalTo(EMAIL))
            .body("name", equalTo(NAME))
            .body("id", notNullValue());
    }

    @Test
    @Order(2)
    @DisplayName("Should fail with duplicate email")
    void shouldFailWithDuplicateEmail() {
        given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "name": "%s",
                    "email": "%s",
                    "password": "%s",
                    "roles": ["USER"]
                }
                """, NAME, EMAIL, PASSWORD))
        .when()
            .post(AUTH_BASE + "/signup")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(3)
    @DisplayName("Should login with correct credentials")
    void shouldLoginWithCorrectCredentials() {
        String loginToken = given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, EMAIL, PASSWORD))
        .when()
            .post(AUTH_BASE + "/login")
        .then()
            .statusCode(200)
            .body("token", notNullValue())
            .extract()
            .jsonPath()
            .getString("token");

        assertThat(loginToken).isNotBlank();
    }

    @Test
    @Order(4)
    @DisplayName("Should fail login with wrong password")
    void shouldFailLoginWithWrongPassword() {
        given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "email": "%s",
                    "password": "WrongPass@123"
                }
                """, EMAIL))
        .when()
            .post(AUTH_BASE + "/login")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(5)
    @DisplayName("Should fail login with non-existent email")
    void shouldFailLoginWithNonExistentEmail() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "nonexistent@nowhere.com",
                    "password": "Test@12345"
                }
                """)
        .when()
            .post(AUTH_BASE + "/login")
        .then()
            .statusCode(404);
    }
}
