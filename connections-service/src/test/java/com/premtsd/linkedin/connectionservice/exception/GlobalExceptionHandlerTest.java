package com.premtsd.linkedin.connectionservice.exception;

import com.premtsd.linkedin.connectionservice.controller.ConnectionsController;
import com.premtsd.linkedin.connectionservice.service.ConnectionsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConnectionsController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConnectionsService connectionsService;

    private static final String X_USER_ID_HEADER = "X-User-Id";

    @Test
    void handleBusinessRuleViolationException_ShouldReturnBadRequest() throws Exception {
        // Given
        String errorMessage = "Connection request already exists";
        when(connectionsService.sendConnectionRequest(any()))
            .thenThrow(new BusinessRuleViolationException(errorMessage));

        // When & Then
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(errorMessage))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() throws Exception {
        // Given
        when(connectionsService.sendConnectionRequest(any()))
            .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"));
    }
}
