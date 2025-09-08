package com.premtsd.linkedin.userservice.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PasswordUtilTest {

    @Test
    void hashPassword_ShouldReturnHashedPassword_WhenValidPassword() {
        // Given
        String plainPassword = "password123";

        // When
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        // Then
        assertNotNull(hashedPassword);
        assertNotEquals(plainPassword, hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$")); // BCrypt hash format
    }

    @Test
    void hashPassword_ShouldReturnDifferentHashes_WhenSamePasswordHashedMultipleTimes() {
        // Given
        String plainPassword = "password123";

        // When
        String hash1 = PasswordUtil.hashPassword(plainPassword);
        String hash2 = PasswordUtil.hashPassword(plainPassword);

        // Then
        assertNotEquals(hash1, hash2); // BCrypt uses salt, so hashes should be different
    }

    @Test
    void checkPassword_ShouldReturnTrue_WhenPasswordMatches() {
        // Given
        String plainPassword = "password123";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        // When
        boolean matches = PasswordUtil.checkPassword(plainPassword, hashedPassword);

        // Then
        assertTrue(matches);
    }

    @Test
    void checkPassword_ShouldReturnFalse_WhenPasswordDoesNotMatch() {
        // Given
        String plainPassword = "password123";
        String wrongPassword = "wrongpassword";
        String hashedPassword = PasswordUtil.hashPassword(plainPassword);

        // When
        boolean matches = PasswordUtil.checkPassword(wrongPassword, hashedPassword);

        // Then
        assertFalse(matches);
    }

    @Test
    void checkPassword_ShouldHandleNullPlainPassword() {
        // Given
        String hashedPassword = PasswordUtil.hashPassword("password123");

        // When & Then - BCrypt handles null gracefully, returning false
        boolean result = PasswordUtil.checkPassword(null, hashedPassword);
        assertFalse(result);
    }

    @Test
    void checkPassword_ShouldThrowException_WhenHashedPasswordIsNull() {
        // Given
        String plainPassword = "password123";

        // When & Then
        assertThrows(NullPointerException.class, () -> PasswordUtil.checkPassword(plainPassword, null));
    }

    @Test
    void checkPassword_ShouldThrowException_WhenBothPasswordsAreNull() {
        // When & Then
        assertThrows(NullPointerException.class, () -> PasswordUtil.checkPassword(null, null));
    }

    @Test
    void hashPassword_ShouldHandleNullPassword() {
        // When & Then - BCrypt handles null by returning a hash
        String result = PasswordUtil.hashPassword(null);
        assertNotNull(result);
        assertTrue(result.startsWith("$2a$"));
    }

    @Test
    void hashPassword_ShouldHandleEmptyPassword() {
        // Given
        String emptyPassword = "";

        // When
        String hashedPassword = PasswordUtil.hashPassword(emptyPassword);

        // Then
        assertNotNull(hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$"));
    }

    @Test
    void checkPassword_ShouldReturnTrue_WhenEmptyPasswordMatches() {
        // Given
        String emptyPassword = "";
        String hashedPassword = PasswordUtil.hashPassword(emptyPassword);

        // When
        boolean matches = PasswordUtil.checkPassword(emptyPassword, hashedPassword);

        // Then
        assertTrue(matches);
    }

    @Test
    void hashPassword_ShouldHandleLongPassword() {
        // Given
        String longPassword = "a".repeat(1000); // 1000 character password

        // When
        String hashedPassword = PasswordUtil.hashPassword(longPassword);

        // Then
        assertNotNull(hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$"));
    }

    @Test
    void checkPassword_ShouldReturnTrue_WhenLongPasswordMatches() {
        // Given
        String longPassword = "a".repeat(1000);
        String hashedPassword = PasswordUtil.hashPassword(longPassword);

        // When
        boolean matches = PasswordUtil.checkPassword(longPassword, hashedPassword);

        // Then
        assertTrue(matches);
    }

    @Test
    void hashPassword_ShouldHandleSpecialCharacters() {
        // Given
        String specialPassword = "!@#$%^&*()_+-=[]{}|;:,.<>?";

        // When
        String hashedPassword = PasswordUtil.hashPassword(specialPassword);

        // Then
        assertNotNull(hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$"));
    }

    @Test
    void checkPassword_ShouldReturnTrue_WhenSpecialCharacterPasswordMatches() {
        // Given
        String specialPassword = "!@#$%^&*()_+-=[]{}|;:,.<>?";
        String hashedPassword = PasswordUtil.hashPassword(specialPassword);

        // When
        boolean matches = PasswordUtil.checkPassword(specialPassword, hashedPassword);

        // Then
        assertTrue(matches);
    }

    @Test
    void hashPassword_ShouldHandleUnicodeCharacters() {
        // Given
        String unicodePassword = "пароль123密码";

        // When
        String hashedPassword = PasswordUtil.hashPassword(unicodePassword);

        // Then
        assertNotNull(hashedPassword);
        assertTrue(hashedPassword.startsWith("$2a$"));
    }

    @Test
    void checkPassword_ShouldReturnTrue_WhenUnicodePasswordMatches() {
        // Given
        String unicodePassword = "пароль123密码";
        String hashedPassword = PasswordUtil.hashPassword(unicodePassword);

        // When
        boolean matches = PasswordUtil.checkPassword(unicodePassword, hashedPassword);

        // Then
        assertTrue(matches);
    }

    @Test
    void checkPassword_ShouldThrowException_WhenHashIsInvalidFormat() {
        // Given
        String plainPassword = "password123";
        String invalidHash = "not-a-valid-bcrypt-hash";

        // When & Then
        assertThrows(IllegalArgumentException.class, () -> PasswordUtil.checkPassword(plainPassword, invalidHash));
    }
}
