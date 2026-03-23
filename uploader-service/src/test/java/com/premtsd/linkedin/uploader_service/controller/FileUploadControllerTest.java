package com.premtsd.linkedin.uploader_service.controller;

import com.premtsd.linkedin.uploader_service.service.FileUploaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false",
    "eureka.client.enabled=false"
})
@ExtendWith(MockitoExtension.class)
class FileUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileUploaderService fileUploaderService;

    private MockMultipartFile validImageFile;
    private MockMultipartFile emptyFile;
    private MockMultipartFile largeFile;

    @BeforeEach
    void setUp() {
        validImageFile = new MockMultipartFile(
                "file", "sample.jpg", "image/jpeg", "dummy image content".getBytes());

        emptyFile = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]);

        // Create a large file (simulating 10MB)
        byte[] largeContent = new byte[10 * 1024 * 1024];
        largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeContent);
    }

    @Test
    void uploadImage_ShouldReturnUrl_WhenValidImageUploaded() throws Exception {
        // Given
        String expectedUrl = "https://cloudinary.com/sample.jpg";
        when(fileUploaderService.upload(any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/file")
                        .file(validImageFile)
                        .contentType("multipart/form-data"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));

        verify(fileUploaderService, times(1)).upload(any());
    }

    @Test
    void uploadImage_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Given
        when(fileUploaderService.upload(any())).thenThrow(new RuntimeException("Cloudinary failed"));

        // When & Then
        mockMvc.perform(multipart("/file")
                        .file(validImageFile)
                        .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError());

        verify(fileUploaderService, times(1)).upload(any());
    }

    @Test
    void uploadImage_ShouldReturnInternalServerError_WhenIOExceptionThrown() throws Exception {
        // Given
        when(fileUploaderService.upload(any())).thenThrow(new IOException("File read error"));

        // When & Then
        mockMvc.perform(multipart("/file")
                        .file(validImageFile)
                        .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError());

        verify(fileUploaderService, times(1)).upload(any());
    }

    @Test
    void uploadImage_ShouldHandleEmptyFile() throws Exception {
        // Given
        String expectedUrl = "https://cloudinary.com/empty.jpg";
        when(fileUploaderService.upload(any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/file")
                        .file(emptyFile)
                        .contentType("multipart/form-data"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));

        verify(fileUploaderService, times(1)).upload(any());
    }

    @Test
    void uploadImage_ShouldHandleLargeFile() throws Exception {
        // Given
        String expectedUrl = "https://cloudinary.com/large.jpg";
        when(fileUploaderService.upload(any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/file")
                        .file(largeFile)
                        .contentType("multipart/form-data"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));

        verify(fileUploaderService, times(1)).upload(any());
    }

    @Test
    void uploadImage_ShouldHandleDifferentFileTypes() throws Exception {
        // Given
        MockMultipartFile pngFile = new MockMultipartFile(
                "file", "image.png", "image/png", "png content".getBytes());
        String expectedUrl = "https://cloudinary.com/image.png";
        when(fileUploaderService.upload(any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/file")
                        .file(pngFile)
                        .contentType("multipart/form-data"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));

        verify(fileUploaderService, times(1)).upload(any());
    }

    @Test
    void uploadImage_ShouldReturnError_WhenNoFileProvided() throws Exception {
        // When & Then — GlobalExceptionHandler catches all exceptions as 500
        mockMvc.perform(multipart("/file")
                        .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError());

        verify(fileUploaderService, never()).upload(any());
    }

    @Test
    void uploadImage_ShouldReturnError_WhenWrongParameterName() throws Exception {
        // Given
        MockMultipartFile wrongParamFile = new MockMultipartFile(
                "wrongParam", "sample.jpg", "image/jpeg", "content".getBytes());

        // When & Then — GlobalExceptionHandler catches all exceptions as 500
        mockMvc.perform(multipart("/file")
                        .file(wrongParamFile)
                        .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError());

        verify(fileUploaderService, never()).upload(any());
    }

    @Test
    void uploadImage_ShouldReturnError_WhenWrongContentType() throws Exception {
        // When & Then — GlobalExceptionHandler catches all exceptions as 500
        mockMvc.perform(multipart("/file")
                        .file(validImageFile)
                        .contentType("application/json"))
                .andExpect(status().isInternalServerError());

        verify(fileUploaderService, never()).upload(any());
    }

    @Test
    void uploadImage_ShouldHandleSpecialCharactersInFilename() throws Exception {
        // Given
        MockMultipartFile specialFile = new MockMultipartFile(
                "file", "special-file_name (1).jpg", "image/jpeg", "content".getBytes());
        String expectedUrl = "https://cloudinary.com/special-file.jpg";
        when(fileUploaderService.upload(any())).thenReturn(expectedUrl);

        // When & Then
        mockMvc.perform(multipart("/file")
                        .file(specialFile)
                        .contentType("multipart/form-data"))
                .andExpect(status().isOk())
                .andExpect(content().string(expectedUrl));

        verify(fileUploaderService, times(1)).upload(any());
    }
}
