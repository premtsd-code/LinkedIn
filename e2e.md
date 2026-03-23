# Prompt for Claude — E2E Tests (REST Assured)
# Give this entire file to Claude in IntelliJ

---

You are helping me implement End-to-End (E2E) API tests
for my LinkedIn microservices project using REST Assured.

## Project Context

- Language: Java 17 (Amazon Corretto)
- Framework: Spring Boot 3.2.x
- Build: Maven (multi-module project)
- Package prefix: com.premtsd.linkedin
- DEV environment running at: http://195.201.195.25:10000
- All services running via Docker Compose on Hetzner server

## Running Services (DEV environment)

| Service              | Internal Port | External (DEV) |
|----------------------|---------------|----------------|
| api-gateway          | 8080          | 10000          |
| discovery-server     | 8761          | 10001          |
| notification-service | 9293          | 10003          |
| postgres             | 5432          | 10100          |
| neo4j browser        | 7474          | 10101          |
| neo4j bolt           | 7687          | 10102          |
| redis                | 6379          | 10103          |
| kafbat-ui            | 8080          | 10201          |
| zipkin               | 9411          | 10300          |
| opensearch           | 9200          | 10301          |
| opensearch-dashboards| 5601          | 10302          |

## Base URL for all E2E tests

```
http://195.201.195.25:10000
```

All API calls go through the API Gateway on port 10000.

---

## Task 1 — Create e2e-tests Maven Module

Create a new Maven module called `e2e-tests` in the root project.

### e2e-tests/pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.premtsd.linkedin</groupId>
        <artifactId>linkedin-parent</artifactId>
        <version>1.0.0</version>
    </parent>

    <artifactId>e2e-tests</artifactId>
    <name>E2E Tests</name>

    <dependencies>
        <!-- REST Assured -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>rest-assured</artifactId>
            <version>5.4.0</version>
            <scope>test</scope>
        </dependency>

        <!-- REST Assured JSON path -->
        <dependency>
            <groupId>io.rest-assured</groupId>
            <artifactId>json-path</artifactId>
            <version>5.4.0</version>
            <scope>test</scope>
        </dependency>

        <!-- JUnit 5 -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- AssertJ -->
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Jackson for JSON -->
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <configuration>
                    <!-- Skip E2E tests in normal build -->
                    <skipTests>${skipE2ETests}</skipTests>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## Task 2 — Base E2E Test Configuration

Create base config class:

```
e2e-tests/src/test/java/
  com/premtsd/linkedin/e2e/
    config/
      E2ETestConfig.java
    helper/
      AuthHelper.java
      TestDataHelper.java
    journey/
      UserJourneyTest.java
      PostJourneyTest.java
      ConnectionJourneyTest.java
      FileUploadJourneyTest.java
      SecurityJourneyTest.java
```

### E2ETestConfig.java

```java
package com.premtsd.linkedin.e2e.config;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;

public abstract class E2ETestConfig {

    protected static final String BASE_URL =
        System.getProperty(
            "e2e.base.url",
            "http://195.201.195.25:10000"
        );

    protected static final String AUTH_BASE =
        BASE_URL + "/api/v1/auth";
    protected static final String USERS_BASE =
        BASE_URL + "/api/v1/users";
    protected static final String POSTS_BASE =
        BASE_URL + "/api/v1/posts";
    protected static final String CONNECTIONS_BASE =
        BASE_URL + "/api/v1/connections";
    protected static final String NOTIFICATIONS_BASE =
        BASE_URL + "/api/v1/notifications";
    protected static final String UPLOAD_BASE =
        BASE_URL + "/api/v1/upload";

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
```

### AuthHelper.java

```java
package com.premtsd.linkedin.e2e.helper;

import io.restassured.http.ContentType;
import java.util.UUID;
import static io.restassured.RestAssured.given;

public class AuthHelper {

    private static final String AUTH_URL =
        System.getProperty(
            "e2e.base.url",
            "http://195.201.195.25:10000"
        ) + "/api/v1/auth";

    public static String registerAndGetToken(
            String email,
            String password,
            String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "email": "%s",
                    "password": "%s",
                    "name": "%s"
                }
                """, email, password, name))
        .when()
            .post(AUTH_URL + "/register")
        .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getString("token");
    }

    public static String loginAndGetToken(
            String email,
            String password) {
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
        return "test_" +
            UUID.randomUUID().toString()
                .substring(0, 8) +
            "@e2etest.com";
    }

    public static String bearerToken(String token) {
        return "Bearer " + token;
    }
}
```

---

## Task 3 — Journey 1: User Registration + Login

### UserJourneyTest.java

```java
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
    private static final String EMAIL =
        AuthHelper.uniqueEmail();
    private static final String PASSWORD = "Test@12345";
    private static final String NAME = "E2E Test User";

    @Test
    @Order(1)
    @DisplayName("Should register new user successfully")
    void shouldRegisterNewUser() {
        token = given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "email": "%s",
                    "password": "%s",
                    "name": "%s"
                }
                """, EMAIL, PASSWORD, NAME))
        .when()
            .post(AUTH_BASE + "/register")
        .then()
            .statusCode(201)
            .body("token", notNullValue())
            .body("email", equalTo(EMAIL))
            .body("name", equalTo(NAME))
            .extract()
            .jsonPath()
            .getString("token");

        assertThat(token).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("Should fail with duplicate email")
    void shouldFailWithDuplicateEmail() {
        given()
            .contentType(ContentType.JSON)
            .body(String.format("""
                {
                    "email": "%s",
                    "password": "%s",
                    "name": "%s"
                }
                """, EMAIL, PASSWORD, NAME))
        .when()
            .post(AUTH_BASE + "/register")
        .then()
            .statusCode(409);
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
            .statusCode(401);
    }

    @Test
    @Order(5)
    @DisplayName("Should get user profile with valid token")
    void shouldGetUserProfile() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
        .when()
            .get(USERS_BASE + "/me")
        .then()
            .statusCode(200)
            .body("email", equalTo(EMAIL))
            .body("name", equalTo(NAME));
    }

    @Test
    @Order(6)
    @DisplayName("Should fail with invalid registration data")
    void shouldFailWithInvalidEmail() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "email": "not-an-email",
                    "password": "Test@12345",
                    "name": "Test"
                }
                """)
        .when()
            .post(AUTH_BASE + "/register")
        .then()
            .statusCode(400);
    }
}
```

---

## Task 4 — Journey 2: Post Lifecycle

### PostJourneyTest.java

```java
package com.premtsd.linkedin.e2e.journey;

import com.premtsd.linkedin.e2e.config.E2ETestConfig;
import com.premtsd.linkedin.e2e.helper.AuthHelper;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Journey 2: Post Lifecycle")
class PostJourneyTest extends E2ETestConfig {

    private static String token;
    private static Integer postId;

    @BeforeAll
    static void registerUser() {
        token = AuthHelper.registerAndGetToken(
            AuthHelper.uniqueEmail(),
            "Test@12345",
            "Post Test User"
        );
    }

    @Test
    @Order(1)
    @DisplayName("Should create post successfully")
    void shouldCreatePost() {
        postId = given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
            .contentType(ContentType.JSON)
            .body("""
                {
                    "content": "My first E2E test post!"
                }
                """)
        .when()
            .post(POSTS_BASE)
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("content",
                equalTo("My first E2E test post!"))
            .extract()
            .jsonPath()
            .getInt("id");

        assertThat(postId).isPositive();
    }

    @Test
    @Order(2)
    @DisplayName("Should get post by id")
    void shouldGetPostById() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
        .when()
            .get(POSTS_BASE + "/" + postId)
        .then()
            .statusCode(200)
            .body("id", equalTo(postId))
            .body("content",
                equalTo("My first E2E test post!"));
    }

    @Test
    @Order(3)
    @DisplayName("Should appear in user feed")
    void shouldAppearInFeed() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
            .queryParam("page", 0)
            .queryParam("size", 10)
        .when()
            .get(POSTS_BASE + "/feed")
        .then()
            .statusCode(200)
            .body("content.size()", greaterThan(0));
    }

    @Test
    @Order(4)
    @DisplayName("Should like post successfully")
    void shouldLikePost() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
        .when()
            .post(POSTS_BASE + "/" + postId + "/like")
        .then()
            .statusCode(200);
    }

    @Test
    @Order(5)
    @DisplayName("Should not like post twice")
    void shouldNotLikePostTwice() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
        .when()
            .post(POSTS_BASE + "/" + postId + "/like")
        .then()
            .statusCode(409);
    }

    @Test
    @Order(6)
    @DisplayName("Should add comment to post")
    void shouldAddComment() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
            .contentType(ContentType.JSON)
            .body("""
                {"content": "Great post!"}
                """)
        .when()
            .post(POSTS_BASE + "/" + postId + "/comments")
        .then()
            .statusCode(201)
            .body("content", equalTo("Great post!"));
    }

    @Test
    @Order(7)
    @DisplayName("Should update post successfully")
    void shouldUpdatePost() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
            .contentType(ContentType.JSON)
            .body("""
                {"content": "Updated post content!"}
                """)
        .when()
            .put(POSTS_BASE + "/" + postId)
        .then()
            .statusCode(200)
            .body("content",
                equalTo("Updated post content!"));
    }

    @Test
    @Order(8)
    @DisplayName("Should delete post successfully")
    void shouldDeletePost() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
        .when()
            .delete(POSTS_BASE + "/" + postId)
        .then()
            .statusCode(204);
    }

    @Test
    @Order(9)
    @DisplayName("Should return 404 for deleted post")
    void shouldReturn404ForDeletedPost() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
        .when()
            .get(POSTS_BASE + "/" + postId)
        .then()
            .statusCode(404);
    }
}
```

---

## Task 5 — Journey 3: Connection + Notification Flow

### ConnectionJourneyTest.java

```java
package com.premtsd.linkedin.e2e.journey;

import com.premtsd.linkedin.e2e.config.E2ETestConfig;
import com.premtsd.linkedin.e2e.helper.AuthHelper;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Journey 3: Connection and Notification")
class ConnectionJourneyTest extends E2ETestConfig {

    private static String token1;
    private static String token2;
    private static Integer user2Id;

    @BeforeAll
    static void registerTwoUsers() {
        // Register user 1
        token1 = AuthHelper.registerAndGetToken(
            AuthHelper.uniqueEmail(),
            "Test@12345",
            "Connection User 1"
        );

        // Register user 2 + get their ID
        String email2 = AuthHelper.uniqueEmail();
        token2 = AuthHelper.registerAndGetToken(
            email2, "Test@12345", "Connection User 2"
        );

        user2Id = given()
            .header("Authorization",
                AuthHelper.bearerToken(token2))
        .when()
            .get("/api/v1/users/me")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getInt("id");
    }

    @Test
    @Order(1)
    @DisplayName("Should send connection request")
    void shouldSendConnectionRequest() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token1))
        .when()
            .post(CONNECTIONS_BASE + "/" + user2Id)
        .then()
            .statusCode(200);
    }

    @Test
    @Order(2)
    @DisplayName("Should not send duplicate request")
    void shouldNotSendDuplicateRequest() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token1))
        .when()
            .post(CONNECTIONS_BASE + "/" + user2Id)
        .then()
            .statusCode(409);
    }

    @Test
    @Order(3)
    @DisplayName("Should receive notification for request")
    void shouldReceiveNotification()
            throws InterruptedException {
        // Wait for Kafka event processing
        Thread.sleep(2000);

        given()
            .header("Authorization",
                AuthHelper.bearerToken(token2))
        .when()
            .get(NOTIFICATIONS_BASE)
        .then()
            .statusCode(200)
            .body("content.size()", greaterThan(0));
    }

    @Test
    @Order(4)
    @DisplayName("Should accept connection request")
    void shouldAcceptConnectionRequest() {
        Integer requestId = given()
            .header("Authorization",
                AuthHelper.bearerToken(token2))
        .when()
            .get(CONNECTIONS_BASE + "/pending")
        .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getInt("[0].id");

        given()
            .header("Authorization",
                AuthHelper.bearerToken(token2))
        .when()
            .post(CONNECTIONS_BASE +
                "/" + requestId + "/accept")
        .then()
            .statusCode(200);
    }

    @Test
    @Order(5)
    @DisplayName("Should appear in connections list")
    void shouldAppearInConnectionsList() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token1))
        .when()
            .get(CONNECTIONS_BASE)
        .then()
            .statusCode(200)
            .body("size()", greaterThan(0));
    }

    @Test
    @Order(6)
    @DisplayName("Should get mutual connections")
    void shouldGetMutualConnections() {
        given()
            .header("Authorization",
                AuthHelper.bearerToken(token1))
        .when()
            .get(CONNECTIONS_BASE +
                "/" + user2Id + "/mutual")
        .then()
            .statusCode(200);
    }
}
```

---

## Task 6 — Journey 4: File Upload

### FileUploadJourneyTest.java

```java
package com.premtsd.linkedin.e2e.journey;

import com.premtsd.linkedin.e2e.config.E2ETestConfig;
import com.premtsd.linkedin.e2e.helper.AuthHelper;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import java.io.File;
import java.io.FileWriter;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Journey 4: File Upload")
class FileUploadJourneyTest extends E2ETestConfig {

    private static String token;

    @BeforeAll
    static void registerUser() {
        token = AuthHelper.registerAndGetToken(
            AuthHelper.uniqueEmail(),
            "Test@12345",
            "Upload Test User"
        );
    }

    @Test
    @Order(1)
    @DisplayName("Should upload profile picture")
    void shouldUploadProfilePicture()
            throws Exception {
        // Create temp test image file
        File tempFile = File.createTempFile(
            "test-image", ".jpg");
        tempFile.deleteOnExit();

        String fileUrl = given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
            .multiPart("file", tempFile,
                "image/jpeg")
        .when()
            .post(UPLOAD_BASE + "/profile-picture")
        .then()
            .statusCode(200)
            .body("fileUrl", notNullValue())
            .extract()
            .jsonPath()
            .getString("fileUrl");

        assertThat(fileUrl).isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("Should reject invalid file type")
    void shouldRejectInvalidFileType()
            throws Exception {
        File tempFile = File.createTempFile(
            "test", ".exe");
        tempFile.deleteOnExit();

        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
            .multiPart("file", tempFile,
                "application/octet-stream")
        .when()
            .post(UPLOAD_BASE + "/profile-picture")
        .then()
            .statusCode(400);
    }

    @Test
    @Order(3)
    @DisplayName("Should upload post media")
    void shouldUploadPostMedia()
            throws Exception {
        File tempFile = File.createTempFile(
            "post-image", ".png");
        tempFile.deleteOnExit();

        given()
            .header("Authorization",
                AuthHelper.bearerToken(token))
            .multiPart("file", tempFile, "image/png")
        .when()
            .post(UPLOAD_BASE + "/post-media")
        .then()
            .statusCode(200)
            .body("fileUrl", notNullValue());
    }
}
```

---

## Task 7 — Journey 5: Security (Unauthorized Access)

### SecurityJourneyTest.java

```java
package com.premtsd.linkedin.e2e.journey;

import com.premtsd.linkedin.e2e.config.E2ETestConfig;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.given;

@DisplayName("Journey 5: Security and Authorization")
class SecurityJourneyTest extends E2ETestConfig {

    @Test
    @DisplayName("Should return 401 with no token")
    void shouldReturn401WithNoToken() {
        given()
        .when()
            .get(POSTS_BASE + "/feed")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Should return 401 with invalid token")
    void shouldReturn401WithInvalidToken() {
        given()
            .header("Authorization",
                "Bearer invalid.token.here")
        .when()
            .get(USERS_BASE + "/me")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Should return 401 with expired token")
    void shouldReturn401WithExpiredToken() {
        // Expired JWT token (hardcoded for testing)
        String expiredToken =
            "eyJhbGciOiJIUzI1NiJ9." +
            "eyJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwi" +
            "aWF0IjoxNjAwMDAwMDAwLCJleHAiOjE2" +
            "MDAwMDAwMDB9." +
            "invalidSignature";

        given()
            .header("Authorization",
                "Bearer " + expiredToken)
        .when()
            .get(USERS_BASE + "/me")
        .then()
            .statusCode(401);
    }

    @Test
    @DisplayName("Should return 400 with missing fields")
    void shouldReturn400WithMissingFields() {
        given()
            .contentType("application/json")
            .body("{}")
        .when()
            .post(AUTH_BASE + "/register")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Should return 400 with weak password")
    void shouldReturn400WithWeakPassword() {
        given()
            .contentType("application/json")
            .body("""
                {
                    "email": "test@test.com",
                    "password": "weak",
                    "name": "Test"
                }
                """)
        .when()
            .post(AUTH_BASE + "/register")
        .then()
            .statusCode(400);
    }

    @Test
    @DisplayName("Should not access other user profile")
    void shouldNotAccessPrivateData() {
        given()
            .header("Authorization",
                "Bearer invalid")
        .when()
            .delete(POSTS_BASE + "/999")
        .then()
            .statusCode(401);
    }
}
```

---

## Task 8 — Add E2E to GitHub Actions

Add this job to `.github/workflows/develop-ci-cd.yml`
AFTER deploy-dev job:

```yaml
  e2e-tests:
    name: E2E Tests
    runs-on: ubuntu-latest
    needs: deploy-dev
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'corretto'
          cache: maven

      - name: Wait for services to be ready
        run: |
          echo "Waiting for DEV to be ready..."
          sleep 30
          curl -f http://${{ secrets.HETZNER_IP }}:10000/actuator/health
          echo "Services ready!"

      - name: Run E2E tests
        run: |
          mvn test -pl e2e-tests \
            -De2e.base.url=http://${{ secrets.HETZNER_IP }}:10000
        continue-on-error: true

      - name: Upload E2E results
        uses: actions/upload-artifact@v4
        if: always()
        with:
          name: e2e-test-results
          path: e2e-tests/target/surefire-reports/
```

---

## Task 9 — Add to Parent pom.xml modules

Add e2e-tests to parent pom.xml:

```xml
<modules>
    <module>common</module>
    <module>config-server</module>
    <module>discovery-server</module>
    <module>api-gateway</module>
    <module>user-service</module>
    <module>post-service</module>
    <module>connections-service</module>
    <module>notification-service</module>
    <module>uploader-service</module>
    <module>e2e-tests</module>  <!-- ADD THIS -->
</modules>
```

---

## Implementation Rules

- Each test must be independent (use @BeforeAll to setup)
- Use unique emails per test class (AuthHelper.uniqueEmail())
- Add @DisplayName to every test method
- Use @TestMethodOrder + @Order for sequential journeys
- Log all requests/responses via RestAssured filters
- Use assertThat (AssertJ) for assertions
- Use hamcrest matchers for JSON body assertions
- Wait 2 seconds after actions that trigger Kafka events
- All tests target: http://195.201.195.25:10000

## Running E2E Tests Locally

```bash
# Run all E2E tests
mvn test -pl e2e-tests \
  -De2e.base.url=http://195.201.195.25:10000

# Run specific journey
mvn test -pl e2e-tests \
  -Dtest=UserJourneyTest \
  -De2e.base.url=http://195.201.195.25:10000

# Skip E2E in normal build
mvn test -DskipE2ETests=true
```

## Expected Output After Implementation

```
[INFO] Tests run: 6, Failures: 0  — UserJourneyTest
[INFO] Tests run: 9, Failures: 0  — PostJourneyTest
[INFO] Tests run: 6, Failures: 0  — ConnectionJourneyTest
[INFO] Tests run: 3, Failures: 0  — FileUploadJourneyTest
[INFO] Tests run: 6, Failures: 0  — SecurityJourneyTest
[INFO] Total tests: 30, Failures: 0

BUILD SUCCESS
```

Please implement all tasks above starting with Task 1.
Ask me to share any existing service code if needed.