package com.premtsd.linkedin.e2e.journey;

import com.premtsd.linkedin.e2e.config.E2ETestConfig;
import com.premtsd.linkedin.e2e.helper.AuthHelper;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Journey 4: File Upload")
class FileUploadJourneyTest extends E2ETestConfig {

    private static String token;

    @BeforeAll
    static void registerUser() {
        token = AuthHelper.registerAndGetToken(
            AuthHelper.uniqueEmail(), "Test@12345", "Upload Test User");
    }

    // Minimal valid 1x1 pixel JPEG
    private static final String VALID_JPEG_BASE64 =
        "/9j/4AAQSkZJRgABAQEASABIAAD/2wBDAP//////////////////////" +
        "////////////////////////////////////////////////////////////" +
        "2wBDAf//////////////////////////////////////////////////////" +
        "////////////////////////////////////////////////////////////" +
        "wAARCAABAAEDASIAAhEBAxEB/8QAFAABAAAAAAAAAAAAAAAAAAAACf/EABQQ" +
        "AQAAAAAAAAAAAAAAAAAAAAD/xAAUAQEAAAAAAAAAAAAAAAAAAAAA/8QAFBEB" +
        "AAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8AVN//2Q==";

    private File createValidJpeg() throws Exception {
        File tempFile = File.createTempFile("test-image", ".jpg");
        tempFile.deleteOnExit();
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(Base64.getDecoder().decode(VALID_JPEG_BASE64));
        }
        return tempFile;
    }

    @Test
    @Order(1)
    @DisplayName("Should upload image file")
    void shouldUploadImage() throws Exception {
        // Given
        File imageFile = createValidJpeg();

        // When / Then
        String fileUrl = given()
            .header("Authorization", AuthHelper.bearerToken(token))
            .multiPart("file", imageFile, "image/jpeg")
        .when()
            .post(UPLOAD_BASE + "/file")
        .then()
            .statusCode(200)
            .extract()
            .asString();

        assertThat(fileUrl).isNotBlank();
        assertThat(fileUrl).contains("cloudinary.com");
    }

    @Test
    @Order(2)
    @DisplayName("Should upload second image successfully")
    void shouldUploadSecondImage() throws Exception {
        // Given
        File imageFile = createValidJpeg();

        // When / Then
        given()
            .header("Authorization", AuthHelper.bearerToken(token))
            .multiPart("file", imageFile, "image/jpeg")
        .when()
            .post(UPLOAD_BASE + "/file")
        .then()
            .statusCode(200);
    }

    @Test
    @Order(3)
    @DisplayName("Should handle upload without auth")
    void shouldRejectWithoutAuth() throws Exception {
        File tempFile = File.createTempFile("test-image", ".jpg");
        tempFile.deleteOnExit();

        given()
            .multiPart("file", tempFile, "image/jpeg")
        .when()
            .post(UPLOAD_BASE + "/file")
        .then()
            .statusCode(401);
    }
}
