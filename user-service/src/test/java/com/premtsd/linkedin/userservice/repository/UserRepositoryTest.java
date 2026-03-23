package com.premtsd.linkedin.userservice.repository;

import com.premtsd.linkedin.userservice.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.config.import=optional:configserver:",
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "jwt.secretKey=test-secret-key-that-is-at-least-32-characters-long-for-hmac"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    private User createAndSaveUser(String name, String email) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword("hashedPassword123");
        user.setRoles(new HashSet<>());
        return userRepository.save(user);
    }

    @Test
    void shouldFindUserByEmailWhenUserExists() {
        // Given
        User savedUser = createAndSaveUser("John Doe", "john@example.com");

        // When
        Optional<User> foundUser = userRepository.findByEmail("john@example.com");

        // Then
        assertThat(foundUser).isPresent();
        assertThat(foundUser.get().getId()).isEqualTo(savedUser.getId());
        assertThat(foundUser.get().getName()).isEqualTo("John Doe");
        assertThat(foundUser.get().getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void shouldReturnTrueWhenEmailExists() {
        // Given
        createAndSaveUser("Jane Doe", "jane@example.com");

        // When
        boolean exists = userRepository.existsByEmail("jane@example.com");

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    void shouldReturnFalseWhenEmailDoesNotExist() {
        // Given - no user saved with this email

        // When
        boolean exists = userRepository.existsByEmail("nonexistent@example.com");

        // Then
        assertThat(exists).isFalse();
    }

    @Test
    void shouldReturnEmptyOptionalWhenEmailNotFound() {
        // Given - no user saved with this email

        // When
        Optional<User> foundUser = userRepository.findByEmail("nobody@example.com");

        // Then
        assertThat(foundUser).isEmpty();
    }
}
