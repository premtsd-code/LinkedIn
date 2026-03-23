package com.premtsd.linkedin.e2e.journey;

import com.premtsd.linkedin.e2e.config.E2ETestConfig;
import com.premtsd.linkedin.e2e.helper.AuthHelper;
import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Journey 3: Connection and Notification Flow")
class ConnectionJourneyTest extends E2ETestConfig {

    private static String token1;
    private static String token2;
    private static Integer user1Id;
    private static Integer user2Id;

    @BeforeAll
    static void registerTwoUsers() {
        // Register user 1
        String email1 = AuthHelper.uniqueEmail();
        token1 = AuthHelper.registerAndGetToken(
            email1, "Test@12345", "Connection User 1");

        // Register user 2
        String email2 = AuthHelper.uniqueEmail();
        token2 = AuthHelper.registerAndGetToken(
            email2, "Test@12345", "Connection User 2");

        // Extract user IDs from login response
        user1Id = given()
            .contentType("application/json")
            .body(String.format("""
                {"email": "%s", "password": "Test@12345"}
                """, email1))
        .when()
            .post(AUTH_BASE + "/login")
        .then()
            .extract().jsonPath().getInt("id");

        user2Id = given()
            .contentType("application/json")
            .body(String.format("""
                {"email": "%s", "password": "Test@12345"}
                """, email2))
        .when()
            .post(AUTH_BASE + "/login")
        .then()
            .extract().jsonPath().getInt("id");
    }

    @Test
    @Order(1)
    @DisplayName("Should have no connections initially")
    void shouldHaveNoConnectionsInitially() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token1))
        .when()
            .get(CONNECTIONS_BASE + "/first-degree")
        .then()
            .statusCode(200)
            .body("size()", equalTo(0));
    }

    @Test
    @Order(2)
    @DisplayName("Should send connection request")
    void shouldSendConnectionRequest() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token1))
        .when()
            .post(CONNECTIONS_BASE + "/request/" + user2Id)
        .then()
            .statusCode(200)
            .body(equalTo("true"));
    }

    @Test
    @Order(3)
    @DisplayName("Should not send duplicate request")
    void shouldNotSendDuplicateRequest() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token1))
        .when()
            .post(CONNECTIONS_BASE + "/request/" + user2Id)
        .then()
            .statusCode(400);
    }

    @Test
    @Order(4)
    @DisplayName("Should receive notification for connection request")
    void shouldReceiveNotification() throws InterruptedException {
        // Wait for Kafka event processing
        Thread.sleep(3000);

        given()
            .header("Authorization", AuthHelper.bearerToken(token2))
        .when()
            .get(NOTIFICATIONS_BASE)
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
    }

    @Test
    @Order(5)
    @DisplayName("Should accept connection request")
    void shouldAcceptConnectionRequest() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token2))
        .when()
            .post(CONNECTIONS_BASE + "/accept/" + user1Id)
        .then()
            .statusCode(200)
            .body(equalTo("true"));
    }

    @Test
    @Order(6)
    @DisplayName("Should appear in connections list after accept")
    void shouldAppearInConnectionsList() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token1))
        .when()
            .get(CONNECTIONS_BASE + "/first-degree")
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
    }

    @Test
    @Order(7)
    @DisplayName("Should not send request to self")
    void shouldNotSendRequestToSelf() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token1))
        .when()
            .post(CONNECTIONS_BASE + "/request/" + user1Id)
        .then()
            .statusCode(400);
    }
}
