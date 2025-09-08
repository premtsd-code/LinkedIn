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
import org.springframework.web.multipart.MaxUploadSizeExceededException;

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
        IOException ioException = new IOException("File processing failed");
        when(fileUploaderService.upload(any())).thenThrow(ioException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File upload failed: File processing failed"));
    }

    @Test
    void handleRuntimeException_ShouldReturnInternalServerError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        RuntimeException runtimeException = new RuntimeException("Unexpected error occurred");
        when(fileUploaderService.upload(any())).thenThrow(runtimeException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File upload failed: Unexpected error occurred"));
    }

    @Test
    void handleMaxUploadSizeExceededException_ShouldReturnPayloadTooLarge() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", "content".getBytes());
        MaxUploadSizeExceededException sizeException = new MaxUploadSizeExceededException(1024);
        when(fileUploaderService.upload(any())).thenThrow(sizeException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(content().string("File size exceeds maximum allowed size"));
    }

    @Test
    void handleIllegalArgumentException_ShouldReturnBadRequest() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "invalid.txt", "text/plain", "content".getBytes());
        IllegalArgumentException illegalArgException = new IllegalArgumentException("Invalid file type");
        when(fileUploaderService.upload(any())).thenThrow(illegalArgException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid request: Invalid file type"));
    }

    @Test
    void handleNullPointerException_ShouldReturnInternalServerError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        NullPointerException nullPointerException = new NullPointerException("Null value encountered");
        when(fileUploaderService.upload(any())).thenThrow(nullPointerException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File upload failed: Null value encountered"));
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        Exception genericException = new Exception("Generic error");
        when(fileUploaderService.upload(any())).thenThrow(genericException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File upload failed: Generic error"));
    }

    @Test
    void handleIOException_WithNullMessage_ShouldReturnGenericMessage() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        IOException ioException = new IOException((String) null);
        when(fileUploaderService.upload(any())).thenThrow(ioException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File upload failed: null"));
    }

    @Test
    void handleRuntimeException_WithEmptyMessage_ShouldReturnEmptyMessage() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        RuntimeException runtimeException = new RuntimeException("");
        when(fileUploaderService.upload(any())).thenThrow(runtimeException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string("File upload failed: "));
    }

    @Test
    void handleSecurityException_ShouldReturnForbidden() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        SecurityException securityException = new SecurityException("Access denied");
        when(fileUploaderService.upload(any())).thenThrow(securityException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isForbidden())
                .andExpect(content().string("Access denied: Access denied"));
    }

    @Test
    void handleUnsupportedOperationException_ShouldReturnNotImplemented() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes());
        UnsupportedOperationException unsupportedException = new UnsupportedOperationException("Operation not supported");
        when(fileUploaderService.upload(any())).thenThrow(unsupportedException);

        // When & Then
        mockMvc.perform(multipart("/file")
                .file(file)
                .contentType("multipart/form-data"))
                .andExpect(status().isNotImplemented())
                .andExpect(content().string("Operation not supported: Operation not supported"));
    }
}
