package com.premtsd.linkedin.connectionservice.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.connectionservice.entity.Person;
import com.premtsd.linkedin.connectionservice.repository.PersonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebMvc
@Testcontainers
@EnabledIfEnvironmentVariable(named = "DOCKER_AVAILABLE", matches = "true")
class ConnectionsIntegrationTest {

    @Container
    static Neo4jContainer<?> neo4jContainer = new Neo4jContainer<>("neo4j:5.0")
            .withAdminPassword("testpassword")
            .withReuse(true);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.neo4j.uri", neo4jContainer::getBoltUrl);
        registry.add("spring.neo4j.authentication.username", () -> "neo4j");
        registry.add("spring.neo4j.authentication.password", () -> "testpassword");
        registry.add("spring.kafka.bootstrap-servers", () -> "localhost:9092");
        registry.add("eureka.client.enabled", () -> "false");
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    private static final String X_USER_ID_HEADER = "X-User-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        
        // Clean up database
        personRepository.deleteAll();

        // Create test persons
        Person person1 = Person.builder().userId(1L).name("Alice").build();
        Person person2 = Person.builder().userId(2L).name("Bob").build();
        Person person3 = Person.builder().userId(3L).name("Charlie").build();

        personRepository.save(person1);
        personRepository.save(person2);
        personRepository.save(person3);
    }

    @Test
    void completeConnectionFlow_ShouldWorkEndToEnd() throws Exception {
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

        // Step 3: Try to send same request again (should fail)
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest());

        // Step 4: Accept connection request
        mockMvc.perform(post("/core/accept/{userId}", 1L)
                .header(X_USER_ID_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Step 5: Verify connection exists for both users
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].name").value("Bob"));

        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"));

        // Step 6: Try to send connection request to already connected user (should fail)
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectConnectionRequest_ShouldWorkCorrectly() throws Exception {
        // Step 1: Send connection request
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk());

        // Step 2: Reject connection request
        mockMvc.perform(post("/core/reject/{userId}", 1L)
                .header(X_USER_ID_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        // Step 3: Verify no connection exists
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Step 4: Should be able to send request again after rejection
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk());
    }

    @Test
    void multipleConnections_ShouldWorkCorrectly() throws Exception {
        // User 1 connects to User 2
        mockMvc.perform(post("/core/request/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/core/accept/{userId}", 1L)
                .header(X_USER_ID_HEADER, "2"))
                .andExpect(status().isOk());

        // User 1 connects to User 3
        mockMvc.perform(post("/core/request/{userId}", 3L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/core/accept/{userId}", 1L)
                .header(X_USER_ID_HEADER, "3"))
                .andExpect(status().isOk());

        // Verify User 1 has 2 connections
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Verify User 2 has 1 connection
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1));

        // Verify User 3 has 1 connection
        mockMvc.perform(get("/core/first-degree")
                .header(X_USER_ID_HEADER, "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    void invalidOperations_ShouldReturnAppropriateErrors() throws Exception {
        // Try to accept non-existent request
        mockMvc.perform(post("/core/accept/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest());

        // Try to reject non-existent request
        mockMvc.perform(post("/core/reject/{userId}", 2L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest());

        // Try to send request to self
        mockMvc.perform(post("/core/request/{userId}", 1L)
                .header(X_USER_ID_HEADER, "1"))
                .andExpect(status().isBadRequest());
    }
}
