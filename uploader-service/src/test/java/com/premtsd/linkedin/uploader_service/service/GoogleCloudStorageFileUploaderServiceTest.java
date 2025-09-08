package com.premtsd.linkedin.uploader_service.service;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class GoogleCloudStorageFileUploaderServiceTest {

    @Mock
    private Storage storage;

    private GoogleCloudStorageFileUploaderService fileUploaderService;

    private MockMultipartFile validImageFile;
    private MockMultipartFile emptyFile;
    private MockMultipartFile pngFile;

    private static final String TEST_BUCKET_NAME = "test-bucket";

    @BeforeEach
    void setUp() {
        fileUploaderService = new GoogleCloudStorageFileUploaderService(storage);
        ReflectionTestUtils.setField(fileUploaderService, "bucketName", TEST_BUCKET_NAME);
        
        validImageFile = new MockMultipartFile(
                "file", "test-image.jpg", "image/jpeg", "dummy image content".getBytes());
        
        emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);
        
        pngFile = new MockMultipartFile(
                "file", "test.png", "image/png", "png content".getBytes());
    }

    @Test
    void upload_ShouldReturnGcsUrl_WhenUploadSuccessful() throws IOException {
        // Given
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // When
        String resultUrl = fileUploaderService.upload(validImageFile);

        // Then
        assertNotNull(resultUrl);
        assertTrue(resultUrl.startsWith("https://storage.googleapis.com/" + TEST_BUCKET_NAME + "/"));
        assertTrue(resultUrl.endsWith("-test-image.jpg"));
        
        // Verify storage.create was called with correct parameters
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(storage, times(1)).create(blobInfoCaptor.capture(), bytesCaptor.capture());
        
        BlobInfo capturedBlobInfo = blobInfoCaptor.getValue();
        assertEquals(TEST_BUCKET_NAME, capturedBlobInfo.getBucket());
        assertTrue(capturedBlobInfo.getName().endsWith("-test-image.jpg"));
        assertArrayEquals(validImageFile.getBytes(), bytesCaptor.getValue());
    }

    @Test
    void upload_ShouldGenerateUniqueFilenames_WhenMultipleUploads() throws IOException {
        // Given
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // When
        String url1 = fileUploaderService.upload(validImageFile);
        String url2 = fileUploaderService.upload(validImageFile);

        // Then
        assertNotEquals(url1, url2);
        assertTrue(url1.contains("test-image.jpg"));
        assertTrue(url2.contains("test-image.jpg"));
        
        verify(storage, times(2)).create(any(BlobInfo.class), any(byte[].class));
    }

    @Test
    void upload_ShouldHandleEmptyFile() throws IOException {
        // Given
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // When
        String resultUrl = fileUploaderService.upload(emptyFile);

        // Then
        assertNotNull(resultUrl);
        assertTrue(resultUrl.startsWith("https://storage.googleapis.com/" + TEST_BUCKET_NAME + "/"));
        assertTrue(resultUrl.endsWith("-empty.jpg"));
        
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(storage, times(1)).create(any(BlobInfo.class), bytesCaptor.capture());
        assertEquals(0, bytesCaptor.getValue().length);
    }

    @Test
    void upload_ShouldHandleDifferentImageFormats() throws IOException {
        // Given
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // When
        String resultUrl = fileUploaderService.upload(pngFile);

        // Then
        assertNotNull(resultUrl);
        assertTrue(resultUrl.startsWith("https://storage.googleapis.com/" + TEST_BUCKET_NAME + "/"));
        assertTrue(resultUrl.endsWith("-test.png"));
        
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage, times(1)).create(blobInfoCaptor.capture(), any(byte[].class));
        assertTrue(blobInfoCaptor.getValue().getName().endsWith("-test.png"));
    }

    @Test
    void upload_ShouldHandleLargeFiles() throws IOException {
        // Given
        byte[] largeContent = new byte[5 * 1024 * 1024]; // 5MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeContent);
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // When
        String resultUrl = fileUploaderService.upload(largeFile);

        // Then
        assertNotNull(resultUrl);
        assertTrue(resultUrl.contains("large.jpg"));
        
        ArgumentCaptor<byte[]> bytesCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(storage, times(1)).create(any(BlobInfo.class), bytesCaptor.capture());
        assertEquals(largeContent.length, bytesCaptor.getValue().length);
    }

    @Test
    void upload_ShouldHandleSpecialCharactersInFilename() throws IOException {
        // Given
        MockMultipartFile specialFile = new MockMultipartFile(
                "file", "special-file_name (1).jpg", "image/jpeg", "content".getBytes());
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // When
        String resultUrl = fileUploaderService.upload(specialFile);

        // Then
        assertNotNull(resultUrl);
        assertTrue(resultUrl.contains("special-file_name (1).jpg"));
        
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage, times(1)).create(blobInfoCaptor.capture(), any(byte[].class));
        assertTrue(blobInfoCaptor.getValue().getName().contains("special-file_name (1).jpg"));
    }

    @Test
    void upload_ShouldPropagateRuntimeException_WhenStorageThrowsException() {
        // Given
        RuntimeException storageException = new RuntimeException("GCS connection failed");
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenThrow(storageException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            fileUploaderService.upload(validImageFile);
        });

        assertEquals("GCS connection failed", thrown.getMessage());
        verify(storage, times(1)).create(any(BlobInfo.class), any(byte[].class));
    }

    @Test
    void upload_ShouldHandleEmptyOriginalFilename() throws IOException {
        // Given
        MockMultipartFile fileWithEmptyName = new MockMultipartFile(
                "file", "", "image/jpeg", "content".getBytes());
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // When
        String resultUrl = fileUploaderService.upload(fileWithEmptyName);

        // Then
        assertNotNull(resultUrl);
        assertTrue(resultUrl.startsWith("https://storage.googleapis.com/" + TEST_BUCKET_NAME + "/"));
        assertTrue(resultUrl.contains("-")); // UUID + "-" + empty string

        verify(storage, times(1)).create(any(BlobInfo.class), any(byte[].class));
    }

    @Test
    void upload_ShouldCreateCorrectBlobInfo() throws IOException {
        // Given
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // When
        fileUploaderService.upload(validImageFile);

        // Then
        ArgumentCaptor<BlobInfo> blobInfoCaptor = ArgumentCaptor.forClass(BlobInfo.class);
        verify(storage, times(1)).create(blobInfoCaptor.capture(), any(byte[].class));
        
        BlobInfo capturedBlobInfo = blobInfoCaptor.getValue();
        assertEquals(TEST_BUCKET_NAME, capturedBlobInfo.getBucket());
        assertNotNull(capturedBlobInfo.getName());
        assertTrue(capturedBlobInfo.getName().contains("-"));
        assertTrue(capturedBlobInfo.getName().endsWith("test-image.jpg"));
    }

    @Test
    void upload_ShouldReturnCorrectUrlFormat() throws IOException {
        // Given
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenReturn(null);

        // When
        String resultUrl = fileUploaderService.upload(validImageFile);

        // Then
        String expectedUrlPattern = "https://storage.googleapis.com/" + TEST_BUCKET_NAME + "/";
        assertTrue(resultUrl.startsWith(expectedUrlPattern));
        
        // Extract filename from URL
        String filename = resultUrl.substring(expectedUrlPattern.length());
        assertTrue(filename.matches("^[a-f0-9-]+-test-image\\.jpg$"));
    }

    @Test
    void upload_ShouldHandleRuntimeException_WhenStorageThrowsRuntimeException() {
        // Given
        RuntimeException storageException = new RuntimeException("Unexpected GCS error");
        when(storage.create(any(BlobInfo.class), any(byte[].class))).thenThrow(storageException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            fileUploaderService.upload(validImageFile);
        });

        assertEquals("Unexpected GCS error", thrown.getMessage());
        verify(storage, times(1)).create(any(BlobInfo.class), any(byte[].class));
    }
}
