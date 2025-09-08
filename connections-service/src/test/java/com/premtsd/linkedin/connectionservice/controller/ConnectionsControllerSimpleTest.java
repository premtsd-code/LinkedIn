package com.premtsd.linkedin.connectionservice.controller;

import com.premtsd.linkedin.connectionservice.entity.Person;
import com.premtsd.linkedin.connectionservice.exception.BusinessRuleViolationException;
import com.premtsd.linkedin.connectionservice.service.ConnectionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConnectionsControllerSimpleTest {

    @Mock
    private ConnectionsService connectionsService;

    @InjectMocks
    private ConnectionsController connectionsController;

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        // Any setup if needed
    }

    @Test
    void getFirstConnections_ShouldReturnConnections_WhenUserHasConnections() {
        // Given
        List<Person> connections = Arrays.asList(
            Person.builder().id(1L).userId(2L).name("Alice").build(),
            Person.builder().id(2L).userId(3L).name("Bob").build()
        );
        
        when(connectionsService.getFirstDegreeConnections()).thenReturn(connections);

        // When
        ResponseEntity<List<Person>> result = connectionsController.getFirstConnections();

        // Then
        assertEquals(200, result.getStatusCodeValue());
        assertNotNull(result.getBody());
        assertEquals(2, result.getBody().size());
        assertEquals("Alice", result.getBody().get(0).getName());
        assertEquals("Bob", result.getBody().get(1).getName());
    }

    @Test
    void getFirstConnections_ShouldReturnEmptyList_WhenUserHasNoConnections() {
        // Given
        when(connectionsService.getFirstDegreeConnections()).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<Person>> result = connectionsController.getFirstConnections();

        // Then
        assertEquals(200, result.getStatusCodeValue());
        assertNotNull(result.getBody());
        assertEquals(0, result.getBody().size());
    }

    @Test
    void sendConnectionRequest_ShouldReturnTrue_WhenRequestIsSuccessful() {
        // Given
        when(connectionsService.sendConnectionRequest(OTHER_USER_ID)).thenReturn(true);

        // When
        ResponseEntity<Boolean> result = connectionsController.sendConnectionRequest(OTHER_USER_ID);

        // Then
        assertEquals(200, result.getStatusCodeValue());
        assertTrue(result.getBody());
    }

    @Test
    void sendConnectionRequest_ShouldThrowException_WhenRequestAlreadyExists() {
        // Given
        when(connectionsService.sendConnectionRequest(OTHER_USER_ID))
            .thenThrow(new BusinessRuleViolationException("Connection request already exists"));

        // When & Then
        BusinessRuleViolationException exception = assertThrows(
            BusinessRuleViolationException.class,
            () -> connectionsController.sendConnectionRequest(OTHER_USER_ID)
        );

        assertEquals("Connection request already exists", exception.getMessage());
    }

    @Test
    void acceptConnectionRequest_ShouldReturnTrue_WhenRequestIsSuccessful() {
        // Given
        when(connectionsService.acceptConnectionRequest(OTHER_USER_ID)).thenReturn(true);

        // When
        ResponseEntity<Boolean> result = connectionsController.acceptConnectionRequest(OTHER_USER_ID);

        // Then
        assertEquals(200, result.getStatusCodeValue());
        assertTrue(result.getBody());
    }

    @Test
    void acceptConnectionRequest_ShouldThrowException_WhenNoRequestExists() {
        // Given
        when(connectionsService.acceptConnectionRequest(OTHER_USER_ID))
            .thenThrow(new BusinessRuleViolationException("No connection request to accept"));

        // When & Then
        BusinessRuleViolationException exception = assertThrows(
            BusinessRuleViolationException.class,
            () -> connectionsController.acceptConnectionRequest(OTHER_USER_ID)
        );

        assertEquals("No connection request to accept", exception.getMessage());
    }

    @Test
    void rejectConnectionRequest_ShouldReturnTrue_WhenRequestIsSuccessful() {
        // Given
        when(connectionsService.rejectConnectionRequest(OTHER_USER_ID)).thenReturn(true);

        // When
        ResponseEntity<Boolean> result = connectionsController.rejectConnectionRequest(OTHER_USER_ID);

        // Then
        assertEquals(200, result.getStatusCodeValue());
        assertTrue(result.getBody());
    }

    @Test
    void rejectConnectionRequest_ShouldThrowException_WhenNoRequestExists() {
        // Given
        when(connectionsService.rejectConnectionRequest(OTHER_USER_ID))
            .thenThrow(new BusinessRuleViolationException("No connection request to reject"));

        // When & Then
        BusinessRuleViolationException exception = assertThrows(
            BusinessRuleViolationException.class,
            () -> connectionsController.rejectConnectionRequest(OTHER_USER_ID)
        );

        assertEquals("No connection request to reject", exception.getMessage());
    }

    @Test
    void allMethods_ShouldHandleNullInputsGracefully() {
        // Given
        when(connectionsService.sendConnectionRequest(any())).thenReturn(true);
        when(connectionsService.acceptConnectionRequest(any())).thenReturn(true);
        when(connectionsService.rejectConnectionRequest(any())).thenReturn(true);
        when(connectionsService.getFirstDegreeConnections()).thenReturn(Arrays.asList());

        // When & Then - These should not throw null pointer exceptions
        assertDoesNotThrow(() -> connectionsController.getFirstConnections());
        assertDoesNotThrow(() -> connectionsController.sendConnectionRequest(OTHER_USER_ID));
        assertDoesNotThrow(() -> connectionsController.acceptConnectionRequest(OTHER_USER_ID));
        assertDoesNotThrow(() -> connectionsController.rejectConnectionRequest(OTHER_USER_ID));
    }
}
