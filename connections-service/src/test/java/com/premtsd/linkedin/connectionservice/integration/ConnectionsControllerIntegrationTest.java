package com.premtsd.linkedin.connectionservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.connectionservice.entity.Person;
import com.premtsd.linkedin.connectionservice.exception.BusinessRuleViolationException;
import com.premtsd.linkedin.connectionservice.service.ConnectionsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
class ConnectionsControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConnectionsService connectionsService;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String X_USER_ID_HEADER = "X-User-Id";

    @Test
    void completeConnectionFlow_ShouldWorkWithMockedService() throws Exception {
        // Given
        List<Person> emptyConnections = Arrays.asList();
        List<Person> oneConnection = Arrays.asList(
            Person.builder().id(1L).userId(2L).name("Bob").build()
        );

        // Mock the service calls
        when(connectionsService.getFirstDegreeConnections())
            .thenReturn(emptyConnections)  // First call - no connections
            .thenReturn(oneConnection);    // Second call - one connection

        when(connectionsService.sendConnectionRequest(2L)).thenReturn(true);
        when(connectionsService.acceptConnectionRequest(1L)).thenReturn(true);

        // Step 1: Initially no connections
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Step 2: Send connection request
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Step 3: Accept connection request
        mockMvc.perform(post("/core/accept/{userId}", 1L)
                .header(X_USER_ID_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Step 4: Verify connection exists
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].name").value("Bob"));
    }

    @Test
    void businessRuleViolations_ShouldReturnBadRequest() throws Exception {
        // Given
        when(connectionsService.sendConnectionRequest(any()))
            .thenThrow(new BusinessRuleViolationException("Connection request already exists"));

        // When & Then
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Connection request already exists"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void rejectConnectionRequest_ShouldWork() throws Exception {
        // Given
        when(connectionsService.rejectConnectionRequest(1L)).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/core/reject/{userId}", 1L)
                .header(X_USER_ID_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void getAllEndpoints_ShouldBeAccessible() throws Exception {
        // Given
        when(connectionsService.getFirstDegreeConnections()).thenReturn(Arrays.asList());
        when(connectionsService.sendConnectionRequest(any())).thenReturn(true);
        when(connectionsService.acceptConnectionRequest(any())).thenReturn(true);
        when(connectionsService.rejectConnectionRequest(any())).thenReturn(true);

        // Test all endpoints
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/core/accept/{userId}", 1L)
                .header(X_USER_ID_HEADER, "2"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/core/reject/{userId}", 1L)
                .header(X_USER_ID_HEADER, "2"))
                .andExpect(status().isOk());
    }

    @Test
    void errorHandling_ShouldReturnProperStatusCodes() throws Exception {
        // Given
        when(connectionsService.sendConnectionRequest(any()))
            .thenThrow(new RuntimeException("Database error"));

        // When & Then
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.status").value(500));
    }
}
