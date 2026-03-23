package com.premtsd.linkedin.e2e.journey;

import com.premtsd.linkedin.e2e.config.E2ETestConfig;
import com.premtsd.linkedin.e2e.helper.AuthHelper;
import org.junit.jupiter.api.*;

import java.io.File;

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
            AuthHelper.uniqueEmail(), "Test@12345", "Post Test User");
    }

    @Test
    @Order(1)
    @DisplayName("Should create post with image successfully")
    void shouldCreatePost() throws Exception {
        // Given — create a temp image file for multipart upload
        File tempFile = File.createTempFile("test-image", ".jpg");
        tempFile.deleteOnExit();

        // When / Then
        postId = given()
            .header("Authorization", AuthHelper.bearerToken(token))
            .multiPart("content", "My first E2E test post!")
            .multiPart("file", tempFile, "image/jpeg")
        .when()
            .post(POSTS_BASE + "/core")
        .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("content", equalTo("My first E2E test post!"))
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
            .header("Authorization", AuthHelper.bearerToken(token))
        .when()
            .get(POSTS_BASE + "/core/" + postId)
        .then()
            .statusCode(200)
            .body("id", equalTo(postId))
            .body("content", equalTo("My first E2E test post!"));
    }

    @Test
    @Order(3)
    @DisplayName("Should get all posts for user")
    void shouldGetAllPostsForUser() {
        // Extract userId from token claims or just check list is non-empty
        given()
            .header("Authorization", AuthHelper.bearerToken(token))
        .when()
            .get(POSTS_BASE + "/core/users/1/allPosts")
        .then()
            .statusCode(200);
    }

    @Test
    @Order(4)
    @DisplayName("Should like post successfully")
    void shouldLikePost() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token))
        .when()
            .post(POSTS_BASE + "/likes/" + postId)
        .then()
            .statusCode(204);
    }

    @Test
    @Order(5)
    @DisplayName("Should not like post twice")
    void shouldNotLikePostTwice() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token))
        .when()
            .post(POSTS_BASE + "/likes/" + postId)
        .then()
            .statusCode(400);
    }

    @Test
    @Order(6)
    @DisplayName("Should unlike post successfully")
    void shouldUnlikePost() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token))
        .when()
            .delete(POSTS_BASE + "/likes/" + postId)
        .then()
            .statusCode(204);
    }

    @Test
    @Order(7)
    @DisplayName("Should return 404 for non-existent post")
    void shouldReturn404ForNonExistentPost() {
        given()
            .header("Authorization", AuthHelper.bearerToken(token))
        .when()
            .get(POSTS_BASE + "/core/999999")
        .then()
            .statusCode(404);
    }
}
