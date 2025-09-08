package com.premtsd.linkedin.postservice.service;

import com.premtsd.linkedin.postservice.clients.UploaderClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class UploaderServiceWrapperTest {

    @Mock
    private UploaderClient uploaderClient;

    private UploaderServiceWrapper uploaderServiceWrapper;

    private MockMultipartFile testFile;

    @BeforeEach
    void setUp() {
        uploaderServiceWrapper = new UploaderServiceWrapper(uploaderClient);
        
        testFile = new MockMultipartFile(
                "file", 
                "test-image.jpg", 
                "image/jpeg", 
                "test image content".getBytes()
        );
    }

    @Test
    void uploadFile_ShouldReturnUrl_WhenUploadSuccessful() {
        // Given
        String expectedUrl = "https://uploaded.com/test-image.jpg";
        when(uploaderClient.uploadFile(testFile)).thenReturn(expectedUrl);

        // When
        String result = uploaderServiceWrapper.uploadFile(testFile);

        // Then
        assertEquals(expectedUrl, result);
        verify(uploaderClient).uploadFile(testFile);
    }

    @Test
    void uploadFile_ShouldHandleNullFile() {
        // Given
        MockMultipartFile nullFile = null;
        when(uploaderClient.uploadFile(nullFile)).thenReturn("https://uploaded.com/null.jpg");

        // When
        String result = uploaderServiceWrapper.uploadFile(nullFile);

        // Then
        assertEquals("https://uploaded.com/null.jpg", result);
        verify(uploaderClient).uploadFile(nullFile);
    }

    @Test
    void uploadFile_ShouldHandleEmptyFile() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", 
                "empty.jpg", 
                "image/jpeg", 
                new byte[0]
        );
        String expectedUrl = "https://uploaded.com/empty.jpg";
        when(uploaderClient.uploadFile(emptyFile)).thenReturn(expectedUrl);

        // When
        String result = uploaderServiceWrapper.uploadFile(emptyFile);

        // Then
        assertEquals(expectedUrl, result);
        verify(uploaderClient).uploadFile(emptyFile);
    }

    @Test
    void uploadFile_ShouldHandleLargeFile() {
        // Given
        byte[] largeContent = new byte[5 * 1024 * 1024]; // 5MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", 
                "large-image.jpg", 
                "image/jpeg", 
                largeContent
        );
        String expectedUrl = "https://uploaded.com/large-image.jpg";
        when(uploaderClient.uploadFile(largeFile)).thenReturn(expectedUrl);

        // When
        String result = uploaderServiceWrapper.uploadFile(largeFile);

        // Then
        assertEquals(expectedUrl, result);
        verify(uploaderClient).uploadFile(largeFile);
    }

    @Test
    void uploadFile_ShouldHandleDifferentFileTypes() {
        // Given
        MockMultipartFile pngFile = new MockMultipartFile(
                "file", 
                "test.png", 
                "image/png", 
                "png content".getBytes()
        );
        String expectedUrl = "https://uploaded.com/test.png";
        when(uploaderClient.uploadFile(pngFile)).thenReturn(expectedUrl);

        // When
        String result = uploaderServiceWrapper.uploadFile(pngFile);

        // Then
        assertEquals(expectedUrl, result);
        verify(uploaderClient).uploadFile(pngFile);
    }

    @Test
    void uploadFile_ShouldPropagateException_WhenClientThrowsException() {
        // Given
        RuntimeException uploadException = new RuntimeException("Upload service unavailable");
        when(uploaderClient.uploadFile(testFile)).thenThrow(uploadException);

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            uploaderServiceWrapper.uploadFile(testFile);
        });
        
        assertEquals("Upload service unavailable", thrown.getMessage());
        verify(uploaderClient).uploadFile(testFile);
    }

    @Test
    void uploadFile_ShouldHandleSpecialCharactersInFilename() {
        // Given
        MockMultipartFile specialFile = new MockMultipartFile(
                "file", 
                "special-file_name (1).jpg", 
                "image/jpeg", 
                "content".getBytes()
        );
        String expectedUrl = "https://uploaded.com/special-file.jpg";
        when(uploaderClient.uploadFile(specialFile)).thenReturn(expectedUrl);

        // When
        String result = uploaderServiceWrapper.uploadFile(specialFile);

        // Then
        assertEquals(expectedUrl, result);
        verify(uploaderClient).uploadFile(specialFile);
    }

    @Test
    void uploadFile_ShouldReturnNullUrl_WhenClientReturnsNull() {
        // Given
        when(uploaderClient.uploadFile(testFile)).thenReturn(null);

        // When
        String result = uploaderServiceWrapper.uploadFile(testFile);

        // Then
        assertNull(result);
        verify(uploaderClient).uploadFile(testFile);
    }

    @Test
    void uploadFile_ShouldReturnEmptyUrl_WhenClientReturnsEmpty() {
        // Given
        when(uploaderClient.uploadFile(testFile)).thenReturn("");

        // When
        String result = uploaderServiceWrapper.uploadFile(testFile);

        // Then
        assertEquals("", result);
        verify(uploaderClient).uploadFile(testFile);
    }

    @Test
    void uploadFile_ShouldHandleFileWithNullOriginalFilename() {
        // Given
        MockMultipartFile fileWithNullName = new MockMultipartFile(
                "file", 
                null, 
                "image/jpeg", 
                "content".getBytes()
        );
        String expectedUrl = "https://uploaded.com/unnamed-file.jpg";
        when(uploaderClient.uploadFile(fileWithNullName)).thenReturn(expectedUrl);

        // When
        String result = uploaderServiceWrapper.uploadFile(fileWithNullName);

        // Then
        assertEquals(expectedUrl, result);
        verify(uploaderClient).uploadFile(fileWithNullName);
    }

    @Test
    void uploadFile_ShouldHandleFileWithEmptyOriginalFilename() {
        // Given
        MockMultipartFile fileWithEmptyName = new MockMultipartFile(
                "file", 
                "", 
                "image/jpeg", 
                "content".getBytes()
        );
        String expectedUrl = "https://uploaded.com/empty-name.jpg";
        when(uploaderClient.uploadFile(fileWithEmptyName)).thenReturn(expectedUrl);

        // When
        String result = uploaderServiceWrapper.uploadFile(fileWithEmptyName);

        // Then
        assertEquals(expectedUrl, result);
        verify(uploaderClient).uploadFile(fileWithEmptyName);
    }

    @Test
    void uploadFile_ShouldHandleMultipleConsecutiveCalls() {
        // Given
        String expectedUrl1 = "https://uploaded.com/file1.jpg";
        String expectedUrl2 = "https://uploaded.com/file2.jpg";
        
        MockMultipartFile file1 = new MockMultipartFile("file", "file1.jpg", "image/jpeg", "content1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("file", "file2.jpg", "image/jpeg", "content2".getBytes());
        
        when(uploaderClient.uploadFile(file1)).thenReturn(expectedUrl1);
        when(uploaderClient.uploadFile(file2)).thenReturn(expectedUrl2);

        // When
        String result1 = uploaderServiceWrapper.uploadFile(file1);
        String result2 = uploaderServiceWrapper.uploadFile(file2);

        // Then
        assertEquals(expectedUrl1, result1);
        assertEquals(expectedUrl2, result2);
        verify(uploaderClient).uploadFile(file1);
        verify(uploaderClient).uploadFile(file2);
    }

    @Test
    void uploadFile_ShouldHandleInterruptedException() {
        // Given
        when(uploaderClient.uploadFile(testFile)).thenAnswer(invocation -> {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread interrupted");
        });

        // When & Then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            uploaderServiceWrapper.uploadFile(testFile);
        });
        
        assertEquals("Thread interrupted", thrown.getMessage());
        assertTrue(Thread.currentThread().isInterrupted());
        verify(uploaderClient).uploadFile(testFile);
    }
}
