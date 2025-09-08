package com.premtsd.linkedin.connectionservice.repository;

import com.premtsd.linkedin.connectionservice.entity.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.neo4j.DataNeo4jTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataNeo4jTest
@Testcontainers
@EnabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "true")
class PersonRepositoryTest {

    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.0")
            .withAdminPassword("testpassword")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
    }

    @Autowired
    private PersonRepository personRepository;

    private Person person1;
    private Person person2;
    private Person person3;

    @BeforeEach
    void setUp() {
        // Clean up database
        personRepository.deleteAll();

        // Create test persons
        person1 = Person.builder().userId(1L).name("Alice").build();
        person2 = Person.builder().userId(2L).name("Bob").build();
        person3 = Person.builder().userId(3L).name("Charlie").build();

        person1 = personRepository.save(person1);
        person2 = personRepository.save(person2);
        person3 = personRepository.save(person3);
    }

    @Test
    void getByName_ShouldReturnPerson_WhenPersonExists() {
        // When
        Optional<Person> result = personRepository.getByName("Alice");

        // Then
        assertTrue(result.isPresent());
        assertEquals("Alice", result.get().getName());
        assertEquals(1L, result.get().getUserId());
    }

    @Test
    void getByName_ShouldReturnEmpty_WhenPersonDoesNotExist() {
        // When
        Optional<Person> result = personRepository.getByName("NonExistent");

        // Then
        assertFalse(result.isPresent());
    }

    @Test
    void connectionRequestExists_ShouldReturnFalse_WhenNoRequestExists() {
        // When
        boolean result = personRepository.connectionRequestExists(1L, 2L);

        // Then
        assertFalse(result);
    }

    @Test
    void connectionRequestExists_ShouldReturnTrue_AfterAddingRequest() {
        // Given
        personRepository.addConnectionRequest(1L, 2L);

        // When
        boolean result = personRepository.connectionRequestExists(1L, 2L);

        // Then
        assertTrue(result);
    }

    @Test
    void alreadyConnected_ShouldReturnFalse_WhenNotConnected() {
        // When
        boolean result = personRepository.alreadyConnected(1L, 2L);

        // Then
        assertFalse(result);
    }

    @Test
    void addConnectionRequest_ShouldCreateRequestRelationship() {
        // When
        personRepository.addConnectionRequest(1L, 2L);

        // Then
        assertTrue(personRepository.connectionRequestExists(1L, 2L));
        assertFalse(personRepository.connectionRequestExists(2L, 1L)); // Should be directional
    }

    @Test
    void acceptConnectionRequest_ShouldCreateConnectionAndRemoveRequest() {
        // Given
        personRepository.addConnectionRequest(1L, 2L);
        assertTrue(personRepository.connectionRequestExists(1L, 2L));

        // When
        personRepository.acceptConnectionRequest(1L, 2L);

        // Then
        assertFalse(personRepository.connectionRequestExists(1L, 2L));
        assertTrue(personRepository.alreadyConnected(1L, 2L));
    }

    @Test
    void rejectConnectionRequest_ShouldRemoveRequest() {
        // Given
        personRepository.addConnectionRequest(1L, 2L);
        assertTrue(personRepository.connectionRequestExists(1L, 2L));

        // When
        personRepository.rejectConnectionRequest(1L, 2L);

        // Then
        assertFalse(personRepository.connectionRequestExists(1L, 2L));
        assertFalse(personRepository.alreadyConnected(1L, 2L));
    }

    @Test
    void getFirstDegreeConnections_ShouldReturnEmptyList_WhenNoConnections() {
        // When
        List<Person> connections = personRepository.getFirstDegreeConnections(1L);

        // Then
        assertTrue(connections.isEmpty());
    }

    @Test
    void getFirstDegreeConnections_ShouldReturnConnections_WhenConnectionsExist() {
        // Given - Create connections: person1 <-> person2, person1 <-> person3
        personRepository.addConnectionRequest(1L, 2L);
        personRepository.acceptConnectionRequest(1L, 2L);
        
        personRepository.addConnectionRequest(3L, 1L);
        personRepository.acceptConnectionRequest(3L, 1L);

        // When
        List<Person> connections = personRepository.getFirstDegreeConnections(1L);

        // Then
        assertEquals(2, connections.size());
        assertTrue(connections.stream().anyMatch(p -> p.getUserId().equals(2L)));
        assertTrue(connections.stream().anyMatch(p -> p.getUserId().equals(3L)));
    }

    @Test
    void getFirstDegreeConnections_ShouldNotReturnRequestedButNotAcceptedConnections() {
        // Given - Create request but don't accept
        personRepository.addConnectionRequest(1L, 2L);

        // When
        List<Person> connections = personRepository.getFirstDegreeConnections(1L);

        // Then
        assertTrue(connections.isEmpty());
    }

    @Test
    void multipleConnectionRequests_ShouldWorkIndependently() {
        // Given
        personRepository.addConnectionRequest(1L, 2L);
        personRepository.addConnectionRequest(2L, 3L);

        // Then
        assertTrue(personRepository.connectionRequestExists(1L, 2L));
        assertTrue(personRepository.connectionRequestExists(2L, 3L));
        assertFalse(personRepository.connectionRequestExists(1L, 3L));
        assertFalse(personRepository.connectionRequestExists(2L, 1L));
    }
}
