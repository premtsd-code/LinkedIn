package com.premtsd.linkedin.connectionservice.repository;

import com.premtsd.linkedin.connectionservice.entity.Person;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PersonRepositoryUnitTest {

    @Mock
    private PersonRepository personRepository;

    @Test
    void getByName_ShouldReturnPerson_WhenPersonExists() {
        // Given
        Person expectedPerson = Person.builder()
                .id(1L)
                .userId(100L)
                .name("Alice")
                .build();
        
        when(personRepository.getByName("Alice")).thenReturn(Optional.of(expectedPerson));

        // When
        Optional<Person> result = personRepository.getByName("Alice");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
        assertEquals(100L, result.get().getUserId());
        verify(personRepository).getByName("Alice");
    }

    @Test
    void getByName_ShouldReturnEmpty_WhenPersonDoesNotExist() {
        // Given
        when(personRepository.getByName("NonExistent")).thenReturn(Optional.empty());

        // When
        Optional<Person> result = personRepository.getByName("NonExistent");

        // Then
        assertFalse(result.isPresent());
        verify(personRepository).getByName("NonExistent");
    }

    @Test
    void getFirstDegreeConnections_ShouldReturnConnections() {
        // Given
        List<Person> expectedConnections = Arrays.asList(
                Person.builder().id(1L).userId(2L).name("Bob").build(),
                Person.builder().id(2L).userId(3L).name("Charlie").build()
        );
        
        when(personRepository.getFirstDegreeConnections(1L)).thenReturn(expectedConnections);

        // When
        List<Person> result = personRepository.getFirstDegreeConnections(1L);

        // Then
        assertEquals(2, result.size());
        assertEquals("Bob", result.get(0).getName());
        assertEquals("Charlie", result.get(1).getName());
        verify(personRepository).getFirstDegreeConnections(1L);
    }

    @Test
    void connectionRequestExists_ShouldReturnTrue_WhenRequestExists() {
        // Given
        when(personRepository.connectionRequestExists(1L, 2L)).thenReturn(true);

        // When
        boolean result = personRepository.connectionRequestExists(1L, 2L);

        // Then
        assertTrue(result);
        verify(personRepository).connectionRequestExists(1L, 2L);
    }

    @Test
    void connectionRequestExists_ShouldReturnFalse_WhenRequestDoesNotExist() {
        // Given
        when(personRepository.connectionRequestExists(1L, 2L)).thenReturn(false);

        // When
        boolean result = personRepository.connectionRequestExists(1L, 2L);

        // Then
        assertFalse(result);
        verify(personRepository).connectionRequestExists(1L, 2L);
    }

    @Test
    void alreadyConnected_ShouldReturnTrue_WhenUsersAreConnected() {
        // Given
        when(personRepository.alreadyConnected(1L, 2L)).thenReturn(true);

        // When
        boolean result = personRepository.alreadyConnected(1L, 2L);

        // Then
        assertTrue(result);
        verify(personRepository).alreadyConnected(1L, 2L);
    }

    @Test
    void alreadyConnected_ShouldReturnFalse_WhenUsersAreNotConnected() {
        // Given
        when(personRepository.alreadyConnected(1L, 2L)).thenReturn(false);

        // When
        boolean result = personRepository.alreadyConnected(1L, 2L);

        // Then
        assertFalse(result);
        verify(personRepository).alreadyConnected(1L, 2L);
    }

    @Test
    void addConnectionRequest_ShouldCallRepository() {
        // When
        personRepository.addConnectionRequest(1L, 2L);

        // Then
        verify(personRepository).addConnectionRequest(1L, 2L);
    }

    @Test
    void acceptConnectionRequest_ShouldCallRepository() {
        // When
        personRepository.acceptConnectionRequest(1L, 2L);

        // Then
        verify(personRepository).acceptConnectionRequest(1L, 2L);
    }

    @Test
    void rejectConnectionRequest_ShouldCallRepository() {
        // When
        personRepository.rejectConnectionRequest(1L, 2L);

        // Then
        verify(personRepository).rejectConnectionRequest(1L, 2L);
    }

    @Test
    void save_ShouldReturnSavedPerson() {
        // Given
        Person personToSave = Person.builder()
                .userId(1L)
                .name("Alice")
                .build();
        
        Person savedPerson = Person.builder()
                .id(1L)
                .userId(1L)
                .name("Alice")
                .build();
        
        when(personRepository.save(personToSave)).thenReturn(savedPerson);

        // When
        Person result = personRepository.save(personToSave);

        // Then
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Alice", result.getName());
        verify(personRepository).save(personToSave);
    }
}
