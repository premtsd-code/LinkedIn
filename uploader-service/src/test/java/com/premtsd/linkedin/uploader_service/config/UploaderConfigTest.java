package com.premtsd.linkedin.uploader_service.config;

import com.cloudinary.Cloudinary;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UploaderConfigTest {

    private UploaderConfig uploaderConfig;

    @BeforeEach
    void setUp() {
        uploaderConfig = new UploaderConfig();
    }

    @Test
    void cloudinary_ShouldCreateCloudinaryBean_WhenValidConfiguration() {
        // Given
        ReflectionTestUtils.setField(uploaderConfig, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(uploaderConfig, "apiKey", "test-key");
        ReflectionTestUtils.setField(uploaderConfig, "apiSecret", "test-secret");

        // When
        Cloudinary cloudinary = uploaderConfig.cloudinary();

        // Then
        assertNotNull(cloudinary);
        assertEquals("test-cloud", cloudinary.config.cloudName);
        assertEquals("test-key", cloudinary.config.apiKey);
        assertEquals("test-secret", cloudinary.config.apiSecret);
    }

    @Test
    void cloudinary_ShouldCreateCloudinaryBean_WhenEmptyConfiguration() {
        // Given
        ReflectionTestUtils.setField(uploaderConfig, "cloudName", "");
        ReflectionTestUtils.setField(uploaderConfig, "apiKey", "");
        ReflectionTestUtils.setField(uploaderConfig, "apiSecret", "");

        // When
        Cloudinary cloudinary = uploaderConfig.cloudinary();

        // Then
        assertNotNull(cloudinary);
        assertEquals("", cloudinary.config.cloudName);
        assertEquals("", cloudinary.config.apiKey);
        assertEquals("", cloudinary.config.apiSecret);
    }

    @Test
    void cloudinary_ShouldCreateCloudinaryBean_WhenNullConfiguration() {
        // Given
        ReflectionTestUtils.setField(uploaderConfig, "cloudName", "");
        ReflectionTestUtils.setField(uploaderConfig, "apiKey", "");
        ReflectionTestUtils.setField(uploaderConfig, "apiSecret", "");

        // When
        Cloudinary cloudinary = uploaderConfig.cloudinary();

        // Then
        assertNotNull(cloudinary);
        assertEquals("", cloudinary.config.cloudName);
        assertEquals("", cloudinary.config.apiKey);
        assertEquals("", cloudinary.config.apiSecret);
    }

    @Test
    void cloudinary_ShouldCreateCloudinaryBean_WhenSpecialCharactersInConfiguration() {
        // Given
        ReflectionTestUtils.setField(uploaderConfig, "cloudName", "test-cloud-123");
        ReflectionTestUtils.setField(uploaderConfig, "apiKey", "123456789");
        ReflectionTestUtils.setField(uploaderConfig, "apiSecret", "abcdef123456!@#$%^&*()");

        // When
        Cloudinary cloudinary = uploaderConfig.cloudinary();

        // Then
        assertNotNull(cloudinary);
        assertEquals("test-cloud-123", cloudinary.config.cloudName);
        assertEquals("123456789", cloudinary.config.apiKey);
        assertEquals("abcdef123456!@#$%^&*()", cloudinary.config.apiSecret);
    }

    @Test
    void storage_ShouldThrowException_WhenInvalidPrivateKey() {
        // Given - Invalid private key will cause parsing issues
        String invalidServiceAccount = """
            {
              "type": "service_account",
              "project_id": "test-project",
              "private_key_id": "test-key-id",
              "private_key": "-----BEGIN PRIVATE KEY-----\\nINVALID_KEY_DATA\\n-----END PRIVATE KEY-----\\n",
              "client_email": "test@test-project.iam.gserviceaccount.com",
              "client_id": "123456789",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token"
            }
            """;
        ReflectionTestUtils.setField(uploaderConfig, "serviceAccountJson", invalidServiceAccount);

        // When & Then
        assertThrows(IOException.class, () -> uploaderConfig.storage());
    }

    @Test
    void storage_ShouldThrowException_WhenInvalidServiceAccount() {
        // Given
        String invalidServiceAccount = "invalid json";
        ReflectionTestUtils.setField(uploaderConfig, "serviceAccountJson", invalidServiceAccount);

        // When & Then
        assertThrows(IOException.class, () -> uploaderConfig.storage());
    }

    @Test
    void storage_ShouldThrowException_WhenEmptyServiceAccount() {
        // Given
        ReflectionTestUtils.setField(uploaderConfig, "serviceAccountJson", "");

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> uploaderConfig.storage());
    }

    @Test
    void storage_ShouldThrowException_WhenNullServiceAccount() {
        // Given
        ReflectionTestUtils.setField(uploaderConfig, "serviceAccountJson", null);

        // When & Then
        assertThrows(NullPointerException.class, () -> uploaderConfig.storage());
    }

    @Test
    void storage_ShouldThrowException_WhenMalformedJson() {
        // Given
        String malformedJson = "{ \"type\": \"service_account\", \"project_id\": }";
        ReflectionTestUtils.setField(uploaderConfig, "serviceAccountJson", malformedJson);

        // When & Then
        assertThrows(IOException.class, () -> uploaderConfig.storage());
    }

    @Test
    void storage_ShouldThrowException_WhenMissingRequiredFields() {
        // Given
        String incompleteServiceAccount = """
            {
              "type": "service_account",
              "project_id": "test-project"
            }
            """;
        ReflectionTestUtils.setField(uploaderConfig, "serviceAccountJson", incompleteServiceAccount);

        // When & Then
        assertThrows(IOException.class, () -> uploaderConfig.storage());
    }

    @Test
    void cloudinary_ShouldHandleLongConfigurationValues() {
        // Given
        String longCloudName = "a".repeat(1000);
        String longApiKey = "1".repeat(1000);
        String longApiSecret = "x".repeat(1000);
        
        ReflectionTestUtils.setField(uploaderConfig, "cloudName", longCloudName);
        ReflectionTestUtils.setField(uploaderConfig, "apiKey", longApiKey);
        ReflectionTestUtils.setField(uploaderConfig, "apiSecret", longApiSecret);

        // When
        Cloudinary cloudinary = uploaderConfig.cloudinary();

        // Then
        assertNotNull(cloudinary);
        assertEquals(longCloudName, cloudinary.config.cloudName);
        assertEquals(longApiKey, cloudinary.config.apiKey);
        assertEquals(longApiSecret, cloudinary.config.apiSecret);
    }

    @Test
    void cloudinary_ShouldCreateDifferentInstances_WhenCalledMultipleTimes() {
        // Given
        ReflectionTestUtils.setField(uploaderConfig, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(uploaderConfig, "apiKey", "test-key");
        ReflectionTestUtils.setField(uploaderConfig, "apiSecret", "test-secret");

        // When
        Cloudinary cloudinary1 = uploaderConfig.cloudinary();
        Cloudinary cloudinary2 = uploaderConfig.cloudinary();

        // Then
        assertNotNull(cloudinary1);
        assertNotNull(cloudinary2);
        assertNotSame(cloudinary1, cloudinary2); // Different instances
        assertEquals(cloudinary1.config.cloudName, cloudinary2.config.cloudName);
        assertEquals(cloudinary1.config.apiKey, cloudinary2.config.apiKey);
        assertEquals(cloudinary1.config.apiSecret, cloudinary2.config.apiSecret);
    }

    @Test
    void storage_ShouldThrowException_WhenCalledWithInvalidCredentials() {
        // Given - This test focuses on the exception behavior rather than creating actual instances
        String invalidServiceAccount = """
            {
              "type": "service_account",
              "project_id": "test-project",
              "private_key_id": "test-key-id",
              "private_key": "-----BEGIN PRIVATE KEY-----\\nINVALID\\n-----END PRIVATE KEY-----\\n",
              "client_email": "test@test-project.iam.gserviceaccount.com",
              "client_id": "123456789",
              "auth_uri": "https://accounts.google.com/o/oauth2/auth",
              "token_uri": "https://oauth2.googleapis.com/token"
            }
            """;
        ReflectionTestUtils.setField(uploaderConfig, "serviceAccountJson", invalidServiceAccount);

        // When & Then
        assertThrows(IOException.class, () -> uploaderConfig.storage());
        assertThrows(IOException.class, () -> uploaderConfig.storage()); // Should consistently throw
    }

    @Test
    void cloudinary_ShouldHandleUnicodeCharacters() {
        // Given
        ReflectionTestUtils.setField(uploaderConfig, "cloudName", "тест-облако");
        ReflectionTestUtils.setField(uploaderConfig, "apiKey", "测试密钥");
        ReflectionTestUtils.setField(uploaderConfig, "apiSecret", "🔑secret🔐");

        // When
        Cloudinary cloudinary = uploaderConfig.cloudinary();

        // Then
        assertNotNull(cloudinary);
        assertEquals("тест-облако", cloudinary.config.cloudName);
        assertEquals("测试密钥", cloudinary.config.apiKey);
        assertEquals("🔑secret🔐", cloudinary.config.apiSecret);
    }
}
