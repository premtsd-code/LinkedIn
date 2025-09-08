package com.premtsd.linkedin.connectionservice.service;

import com.premtsd.linkedin.connectionservice.auth.UserContextHolder;
import com.premtsd.linkedin.connectionservice.entity.Person;
import com.premtsd.linkedin.connectionservice.event.AcceptConnectionRequestEvent;
import com.premtsd.linkedin.connectionservice.event.SendConnectionRequestEvent;
import com.premtsd.linkedin.connectionservice.exception.BusinessRuleViolationException;
import com.premtsd.linkedin.connectionservice.repository.PersonRepository;
import com.premtsd.linkedin.userservice.event.UserCreatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConnectionsServiceTest {

    @Mock
    private PersonRepository personRepository;

    @Mock
    private KafkaTemplate<Long, SendConnectionRequestEvent> sendRequestKafkaTemplate;

    @Mock
    private KafkaTemplate<Long, AcceptConnectionRequestEvent> acceptRequestKafkaTemplate;

    @InjectMocks
    private ConnectionsService connectionsService;

    private static final Long CURRENT_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final String USER_NAME = "John Doe";

    @BeforeEach
    void setUp() {
        // Clear any existing user context
        UserContextHolder.clear();
    }

    @Test
    void handleUserCreated_ShouldCreatePersonSuccessfully() {
        // Given
        UserCreatedEvent event = new UserCreatedEvent();
        event.setUserId(CURRENT_USER_ID);
        event.setName(USER_NAME);

        // When
        connectionsService.handleUserCreated(event);

        // Then
        verify(personRepository).save(argThat(person -> 
            person.getUserId().equals(CURRENT_USER_ID) && 
            person.getName().equals(USER_NAME)
        ));
    }

    @Test
    void getFirstDegreeConnections_ShouldReturnConnections() {
        // Given
        List<Person> expectedConnections = Arrays.asList(
            Person.builder().id(1L).userId(2L).name("Alice").build(),
            Person.builder().id(2L).userId(3L).name("Bob").build()
        );
        
        when(personRepository.getFirstDegreeConnections(CURRENT_USER_ID))
            .thenReturn(expectedConnections);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(CURRENT_USER_ID);

            // When
            List<Person> result = connectionsService.getFirstDegreeConnections();

            // Then
            assertEquals(expectedConnections, result);
            verify(personRepository).getFirstDegreeConnections(CURRENT_USER_ID);
        }
    }

    @Test
    void sendConnectionRequest_ShouldSucceed_WhenValidRequest() {
        // Given
        when(personRepository.connectionRequestExists(CURRENT_USER_ID, OTHER_USER_ID)).thenReturn(false);
        when(personRepository.alreadyConnected(CURRENT_USER_ID, OTHER_USER_ID)).thenReturn(false);

        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<Long, SendConnectionRequestEvent>> future =
            CompletableFuture.completedFuture(mock(SendResult.class));
        when(sendRequestKafkaTemplate.send(anyString(), any(Long.class), any(SendConnectionRequestEvent.class)))
            .thenReturn(future);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(CURRENT_USER_ID);

            // When
            Boolean result = connectionsService.sendConnectionRequest(OTHER_USER_ID);

            // Then
            assertTrue(result);
            verify(personRepository).addConnectionRequest(CURRENT_USER_ID, OTHER_USER_ID);
            verify(sendRequestKafkaTemplate).send(eq("send-connection-request-topic"), eq(OTHER_USER_ID), any(SendConnectionRequestEvent.class));
        }
    }

    @Test
    void sendConnectionRequest_ShouldThrowException_WhenRequestAlreadyExists() {
        // Given
        when(personRepository.connectionRequestExists(CURRENT_USER_ID, OTHER_USER_ID)).thenReturn(true);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(CURRENT_USER_ID);

            // When & Then
            BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> connectionsService.sendConnectionRequest(OTHER_USER_ID)
            );

            assertEquals("Connection request already exists", exception.getMessage());
            verify(personRepository, never()).addConnectionRequest(any(), any());
        }
    }

    @Test
    void sendConnectionRequest_ShouldThrowException_WhenAlreadyConnected() {
        // Given
        when(personRepository.connectionRequestExists(CURRENT_USER_ID, OTHER_USER_ID)).thenReturn(false);
        when(personRepository.alreadyConnected(CURRENT_USER_ID, OTHER_USER_ID)).thenReturn(true);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(CURRENT_USER_ID);

            // When & Then
            BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> connectionsService.sendConnectionRequest(OTHER_USER_ID)
            );

            assertEquals("Users are already connected", exception.getMessage());
            verify(personRepository, never()).addConnectionRequest(any(), any());
        }
    }

    @Test
    void sendConnectionRequest_ShouldThrowException_WhenSendingToSelf() {
        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(CURRENT_USER_ID);

            // When & Then
            BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> connectionsService.sendConnectionRequest(CURRENT_USER_ID)
            );

            assertEquals("Cannot send connection request to yourself", exception.getMessage());
        }
    }

    @Test
    void acceptConnectionRequest_ShouldSucceed_WhenValidRequest() {
        // Given
        when(personRepository.connectionRequestExists(OTHER_USER_ID, CURRENT_USER_ID)).thenReturn(true);

        @SuppressWarnings("unchecked")
        CompletableFuture<SendResult<Long, AcceptConnectionRequestEvent>> future =
            CompletableFuture.completedFuture(mock(SendResult.class));
        when(acceptRequestKafkaTemplate.send(anyString(), any(Long.class), any(AcceptConnectionRequestEvent.class)))
            .thenReturn(future);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(CURRENT_USER_ID);

            // When
            Boolean result = connectionsService.acceptConnectionRequest(OTHER_USER_ID);

            // Then
            assertTrue(result);
            verify(personRepository).acceptConnectionRequest(OTHER_USER_ID, CURRENT_USER_ID);
            verify(acceptRequestKafkaTemplate).send(eq("accept-connection-request-topic"), eq(CURRENT_USER_ID), any(AcceptConnectionRequestEvent.class));
        }
    }

    @Test
    void acceptConnectionRequest_ShouldThrowException_WhenNoRequestExists() {
        // Given
        when(personRepository.connectionRequestExists(OTHER_USER_ID, CURRENT_USER_ID)).thenReturn(false);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(CURRENT_USER_ID);

            // When & Then
            BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> connectionsService.acceptConnectionRequest(OTHER_USER_ID)
            );

            assertEquals("No connection request to accept", exception.getMessage());
            verify(personRepository, never()).acceptConnectionRequest(any(), any());
        }
    }

    @Test
    void rejectConnectionRequest_ShouldSucceed_WhenValidRequest() {
        // Given
        when(personRepository.connectionRequestExists(OTHER_USER_ID, CURRENT_USER_ID)).thenReturn(true);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(CURRENT_USER_ID);

            // When
            Boolean result = connectionsService.rejectConnectionRequest(OTHER_USER_ID);

            // Then
            assertTrue(result);
            verify(personRepository).rejectConnectionRequest(OTHER_USER_ID, CURRENT_USER_ID);
        }
    }

    @Test
    void rejectConnectionRequest_ShouldThrowException_WhenNoRequestExists() {
        // Given
        when(personRepository.connectionRequestExists(OTHER_USER_ID, CURRENT_USER_ID)).thenReturn(false);

        try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
            mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(CURRENT_USER_ID);

            // When & Then
            BusinessRuleViolationException exception = assertThrows(
                BusinessRuleViolationException.class,
                () -> connectionsService.rejectConnectionRequest(OTHER_USER_ID)
            );

            assertEquals("No connection request to reject", exception.getMessage());
            verify(personRepository, never()).rejectConnectionRequest(any(), any());
        }
    }
}
