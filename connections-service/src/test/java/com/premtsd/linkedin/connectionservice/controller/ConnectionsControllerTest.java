package com.premtsd.linkedin.connectionservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.connectionservice.entity.Person;
import com.premtsd.linkedin.connectionservice.exception.BusinessRuleViolationException;
import com.premtsd.linkedin.connectionservice.service.ConnectionsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ConnectionsController.class)
@ActiveProfiles("test")
class ConnectionsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConnectionsService connectionsService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String X_USER_ID_HEADER = "X-User-Id";
    private static final String CURRENT_USER_ID = "1";
    private static final String OTHER_USER_ID = "2";

    @Test
    void getFirstConnections_ShouldReturnConnections_WhenUserHasConnections() throws Exception {
        // Given
        List<Person> connections = Arrays.asList(
            Person.builder().id(1L).userId(2L).name("Alice").build(),
            Person.builder().id(2L).userId(3L).name("Bob").build()
        );
        
        when(connectionsService.getFirstDegreeConnections()).thenReturn(connections);

        // When & Then
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[1].userId").value(3))
                .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    void getFirstConnections_ShouldReturnEmptyList_WhenUserHasNoConnections() throws Exception {
        // Given
        when(connectionsService.getFirstDegreeConnections()).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void sendConnectionRequest_ShouldReturnTrue_WhenRequestIsSuccessful() throws Exception {
        // Given
        when(connectionsService.sendConnectionRequest(2L)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/core/request/{userId}", OTHER_USER_ID)
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void sendConnectionRequest_ShouldReturnBadRequest_WhenRequestAlreadyExists() throws Exception {
        // Given
        when(connectionsService.sendConnectionRequest(2L))
            .thenThrow(new BusinessRuleViolationException("Connection request already exists"));

        // When & Then
        mockMvc.perform(post("/core/request/{userId}", OTHER_USER_ID)
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendConnectionRequest_ShouldReturnBadRequest_WhenUsersAlreadyConnected() throws Exception {
        // Given
        when(connectionsService.sendConnectionRequest(2L))
            .thenThrow(new BusinessRuleViolationException("Users are already connected"));

        // When & Then
        mockMvc.perform(post("/core/request/{userId}", OTHER_USER_ID)
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptConnectionRequest_ShouldReturnTrue_WhenRequestIsSuccessful() throws Exception {
        // Given
        when(connectionsService.acceptConnectionRequest(2L)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/core/accept/{userId}", OTHER_USER_ID)
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void acceptConnectionRequest_ShouldReturnBadRequest_WhenNoRequestExists() throws Exception {
        // Given
        when(connectionsService.acceptConnectionRequest(2L))
            .thenThrow(new BusinessRuleViolationException("No connection request to accept"));

        // When & Then
        mockMvc.perform(post("/core/accept/{userId}", OTHER_USER_ID)
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sendConnectionRequest_ShouldReturnInternalServerError_WhenUserIdIsInvalid() throws Exception {
        // When & Then
        mockMvc.perform(post("/core/request/{userId}", "invalid")
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void acceptConnectionRequest_ShouldReturnInternalServerError_WhenUserIdIsInvalid() throws Exception {
        // When & Then
        mockMvc.perform(post("/core/accept/{userId}", "invalid")
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getFirstConnections_ShouldHandleServiceException() throws Exception {
        // Given
        when(connectionsService.getFirstDegreeConnections())
            .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, CURRENT_USER_ID)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());
    }
}
