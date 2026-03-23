package com.premtsd.linkedin.uploader_service.exception;

import com.premtsd.linkedin.uploader_service.controller.FileUploadController;
import com.premtsd.linkedin.uploader_service.service.FileUploaderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FileUploadController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false",
    "eureka.client.enabled=false"
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileUploaderService fileUploaderService;

    @Test
    void handleIOException_ShouldReturnInternalServerError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        when(fileUploaderService.upload(any())).thenThrow(new IOException("File processing failed"));

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("File processing failed"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void handleRuntimeException_ShouldReturnInternalServerError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        when(fileUploaderService.upload(any())).thenThrow(new RuntimeException("Unexpected error occurred"));

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Unexpected error occurred"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void handleNullPointerException_ShouldReturnInternalServerError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        when(fileUploaderService.upload(any())).thenThrow(new NullPointerException("Null value encountered"));

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Null value encountered"))
                .andExpect(jsonPath("$.status").value(500));
    }

    @Test
    void handleException_ShouldReturnJsonWithErrorDetails() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        when(fileUploaderService.upload(any())).thenThrow(new RuntimeException("Test error"));

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.path").value("/file"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
