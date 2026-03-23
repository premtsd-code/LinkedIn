package com.premtsd.linkedin.e2e.helper;

import io.restassured.http.ContentType;

import java.util.UUID;

import static io.restassured.RestAssured.given;

public class AuthHelper {

    private static final String AUTH_URL =
        System.getProperty("e2e.base.url", "http://195.201.195.25:10000")
            + "/api/v1/users/auth";

    public static String registerAndGetToken(String email, String password, String name) {
        // Step 1: Register (returns UserDto without token)
        given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "name": "%s",
                    "email": "%s",
                    "password": "%s",
                    "roles": ["USER"]
                }
                """, name, email, password))
        .when()
            .post(AUTH_URL + "/signup")
        .then()
            .statusCode(201);

        // Step 2: Login to get token
        return loginAndGetToken(email, password);
    }

    public static String loginAndGetToken(String email, String password) {
        return given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "email": "%s",
                    "password": "%s"
                }
                """, email, password))
        .when()
            .post(AUTH_URL + "/login")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getString("token");
    }

    public static String uniqueEmail() {
        return "test_" + UUID.randomUUID().toString().substring(0, 8) + "@e2etest.com";
    }

    public static String bearerToken(String token) {
        return "Bearer " + token;
    }
}
