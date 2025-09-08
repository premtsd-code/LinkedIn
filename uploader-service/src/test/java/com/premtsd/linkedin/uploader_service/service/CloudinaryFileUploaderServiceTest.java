package com.premtsd.linkedin.uploader_service.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class CloudinaryFileUploaderServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    private CloudinaryFileUploaderService fileUploaderService;

    private MockMultipartFile validImageFile;
    private MockMultipartFile emptyFile;
    private MockMultipartFile pngFile;

    @BeforeEach
    void setUp() {
        when(cloudinary.uploader()).thenReturn(uploader);
        fileUploaderService = new CloudinaryFileUploaderService(cloudinary);

        validImageFile = new MockMultipartFile(
                "file", "test-image.jpg", "image/jpeg", "dummy image content".getBytes());

        emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        pngFile = new MockMultipartFile(
                "file", "test.png", "image/png", "png content".getBytes());
    }

    @Test
    void upload_ShouldReturnSecureUrl_WhenUploadSuccessful() throws Exception {
        // Given
        String expectedUrl = "https://cloudinary.com/test-image.jpg";
        Map<String, Object> mockResult = Map.of("secure_url", expectedUrl);
        when(uploader.upload(validImageFile.getBytes(), Map.of())).thenReturn(mockResult);

        // When
        String resultUrl = fileUploaderService.upload(validImageFile);

        // Then
        assertEquals(expectedUrl, resultUrl);
        verify(uploader, times(1)).upload(validImageFile.getBytes(), Map.of());
        verify(cloudinary, times(1)).uploader();
    }

    @Test
    void upload_ShouldThrowRuntimeException_WhenCloudinaryThrowsIOException() throws Exception {
        // Given
        IOException ioException = new IOException("Cloudinary connection failed");
        when(uploader.upload(validImageFile.getBytes(), Map.of())).thenThrow(ioException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            fileUploaderService.upload(validImageFile);
        });

        assertEquals("Failed to upload file to Cloudinary", thrown.getMessage());
        assertEquals(ioException, thrown.getCause());
        verify(uploader, times(1)).upload(validImageFile.getBytes(), Map.of());
    }

    @Test
    void upload_ShouldHandleEmptyFile() throws Exception {
        // Given
        String expectedUrl = "https://cloudinary.com/empty.jpg";
        Map<String, Object> mockResult = Map.of("secure_url", expectedUrl);
        when(uploader.upload(emptyFile.getBytes(), Map.of())).thenReturn(mockResult);

        // When
        String resultUrl = fileUploaderService.upload(emptyFile);

        // Then
        assertEquals(expectedUrl, resultUrl);
        verify(uploader, times(1)).upload(emptyFile.getBytes(), Map.of());
    }

    @Test
    void upload_ShouldHandleDifferentImageFormats() throws Exception {
        // Given
        String expectedUrl = "https://cloudinary.com/test.png";
        Map<String, Object> mockResult = Map.of("secure_url", expectedUrl);
        when(uploader.upload(pngFile.getBytes(), Map.of())).thenReturn(mockResult);

        // When
        String resultUrl = fileUploaderService.upload(pngFile);

        // Then
        assertEquals(expectedUrl, resultUrl);
        verify(uploader, times(1)).upload(pngFile.getBytes(), Map.of());
    }

    @Test
    void upload_ShouldHandleLargeFiles() throws Exception {
        // Given
        byte[] largeContent = new byte[5 * 1024 * 1024]; // 5MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeContent);
        String expectedUrl = "https://cloudinary.com/large.jpg";
        Map<String, Object> mockResult = Map.of("secure_url", expectedUrl);
        when(uploader.upload(largeContent, Map.of())).thenReturn(mockResult);

        // When
        String resultUrl = fileUploaderService.upload(largeFile);

        // Then
        assertEquals(expectedUrl, resultUrl);
        verify(uploader, times(1)).upload(largeContent, Map.of());
    }

    @Test
    void upload_ShouldHandleSpecialCharactersInFilename() throws Exception {
        // Given
        MockMultipartFile specialFile = new MockMultipartFile(
                "file", "special-file_name (1).jpg", "image/jpeg", "content".getBytes());
        String expectedUrl = "https://cloudinary.com/special-file.jpg";
        Map<String, Object> mockResult = Map.of("secure_url", expectedUrl);
        when(uploader.upload(specialFile.getBytes(), Map.of())).thenReturn(mockResult);

        // When
        String resultUrl = fileUploaderService.upload(specialFile);

        // Then
        assertEquals(expectedUrl, resultUrl);
        verify(uploader, times(1)).upload(specialFile.getBytes(), Map.of());
    }

    @Test
    void upload_ShouldThrowRuntimeException_WhenSecureUrlMissing() throws Exception {
        // Given
        Map<String, Object> mockResult = Map.of("public_id", "test123"); // Missing secure_url
        when(uploader.upload(validImageFile.getBytes(), Map.of())).thenReturn(mockResult);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            fileUploaderService.upload(validImageFile);
        });

        verify(uploader, times(1)).upload(validImageFile.getBytes(), Map.of());
    }

    @Test
    void upload_ShouldThrowRuntimeException_WhenCloudinaryReturnsNull() throws Exception {
        // Given
        when(uploader.upload(validImageFile.getBytes(), Map.of())).thenReturn(null);

        // When & Then
        assertThrows(NullPointerException.class, () -> {
            fileUploaderService.upload(validImageFile);
        });

        verify(uploader, times(1)).upload(validImageFile.getBytes(), Map.of());
    }

    @Test
    void upload_ShouldThrowRuntimeException_WhenGenericExceptionOccurs() throws Exception {
        // Given
        RuntimeException genericException = new RuntimeException("Generic error");
        when(uploader.upload(validImageFile.getBytes(), Map.of())).thenThrow(genericException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            fileUploaderService.upload(validImageFile);
        });

        assertEquals(genericException, thrown);
        verify(uploader, times(1)).upload(validImageFile.getBytes(), Map.of());
    }
}
