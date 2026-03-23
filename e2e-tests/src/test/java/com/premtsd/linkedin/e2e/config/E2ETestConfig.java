package com.premtsd.linkedin.e2e.config;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.BeforeAll;

public abstract class E2ETestConfig {

    protected static final String BASE_URL =
        System.getProperty("e2e.base.url", "http://195.201.195.25:10000");

    protected static final String AUTH_BASE = BASE_URL + "/api/v1/users/auth";
    protected static final String POSTS_BASE = BASE_URL + "/api/v1/posts";
    protected static final String CONNECTIONS_BASE = BASE_URL + "/api/v1/connections/core";
    protected static final String NOTIFICATIONS_BASE = BASE_URL + "/api/v1/notifications/notifications";
    protected static final String UPLOAD_BASE = BASE_URL + "/api/v1/uploads";

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.filters(
            new RequestLoggingFilter(),
            new ResponseLoggingFilter()
        );
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }
}
