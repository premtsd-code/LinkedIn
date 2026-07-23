package com.premtsd.linkedin.userservice.service;

import com.premtsd.linkedin.userservice.dto.LoginRequestDto;
import com.premtsd.linkedin.userservice.dto.SignupRequestDto;
import com.premtsd.linkedin.userservice.dto.UserDto;
import com.premtsd.linkedin.userservice.dto.UserLoginDto;
import com.premtsd.linkedin.userservice.entity.Role;
import com.premtsd.linkedin.userservice.entity.User;
import com.premtsd.linkedin.userservice.event.UserCreatedEmailEvent;
import com.premtsd.linkedin.userservice.event.UserCreatedEvent;
import com.premtsd.linkedin.userservice.exception.BadRequestException;
import com.premtsd.linkedin.userservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.userservice.repository.RoleRepository;
import com.premtsd.linkedin.userservice.repository.UserRepository;
import com.premtsd.linkedin.userservice.utils.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.support.SendResult;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private OutboxService outboxService;

    @InjectMocks
    private AuthService authService;

    private SignupRequestDto signupRequestDto;
    private LoginRequestDto loginRequestDto;
    private User user;
    private Role userRole;
    private UserDto userDto;

    @BeforeEach
    void setUp() {
        // Setup test data
        signupRequestDto = new SignupRequestDto();
        signupRequestDto.setName("John Doe");
        signupRequestDto.setEmail("john.doe@example.com");
        signupRequestDto.setPassword("password123");
        signupRequestDto.setRoles(Set.of("USER"));

        loginRequestDto = new LoginRequestDto();
        loginRequestDto.setEmail("john.doe@example.com");
        loginRequestDto.setPassword("password123");

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        user = new User();
        user.setId(1L);
        user.setName("John Doe");
        user.setEmail("john.doe@example.com");
        user.setPassword("hashedPassword");
        user.setRoles(Set.of(userRole));

        userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("John Doe");
        userDto.setEmail("john.doe@example.com");
    }

    @Test
    void signUp_ShouldCreateUserSuccessfully_WhenValidInput() {
        // Given
        when(userRepository.existsByEmail(signupRequestDto.getEmail())).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(modelMapper.map(signupRequestDto, User.class)).thenReturn(user);
        when(userRepository.save(any(User.class))).thenReturn(user);
        
        // Note: Kafka templates are mocked but not stubbed since we're not verifying their calls

        try (MockedStatic<PasswordUtil> passwordUtilMock = mockStatic(PasswordUtil.class)) {
            passwordUtilMock.when(() -> PasswordUtil.hashPassword(anyString())).thenReturn("hashedPassword");

            // When
            UserDto result = authService.signUp(signupRequestDto);

            // Then
            assertNotNull(result);
            verify(userRepository).existsByEmail(signupRequestDto.getEmail());
            verify(roleRepository).findByName("USER");
            verify(userRepository).save(any(User.class));
            // Note: Kafka event publishing is tested separately in integration tests
        }
    }

    @Test
    void signUp_ShouldThrowException_WhenEmailAlreadyExists() {
        // Given
        when(userRepository.existsByEmail(signupRequestDto.getEmail())).thenReturn(true);

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> authService.signUp(signupRequestDto)
        );

        assertEquals("User already exists, cannot signup again.", exception.getMessage());
        verify(userRepository).existsByEmail(signupRequestDto.getEmail());
        verify(userRepository, never()).save(any());
    }

    @Test
    void signUp_ShouldThrowException_WhenInvalidRoleRequested() {
        // Given
        signupRequestDto.setRoles(Set.of("INVALID_ROLE"));
        when(userRepository.existsByEmail(signupRequestDto.getEmail())).thenReturn(false);
        when(roleRepository.findByName("INVALID_ROLE")).thenReturn(Optional.empty());

        // When & Then
        BadRequestException exception = assertThrows(
            BadRequestException.class,
            () -> authService.signUp(signupRequestDto)
        );

        assertEquals("Role does not exist. - INVALID_ROLE", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_ShouldReturnUserLoginDto_WhenValidCredentials() {
        // Given
        when(userRepository.findByEmail(loginRequestDto.getEmail())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(user)).thenReturn("jwt-token");

        try (MockedStatic<PasswordUtil> passwordUtilMock = mockStatic(PasswordUtil.class)) {
            passwordUtilMock.when(() -> PasswordUtil.checkPassword(loginRequestDto.getPassword(), user.getPassword()))
                .thenReturn(true);

            // When
            UserLoginDto result = authService.login(loginRequestDto);

            // Then
            assertNotNull(result);
            assertEquals("jwt-token", result.getToken());
            assertEquals(user.getId(), result.getId());
            assertEquals(user.getName(), result.getName());
            assertEquals(user.getEmail(), result.getEmail());
            verify(userRepository).findByEmail(loginRequestDto.getEmail());
            verify(jwtService).generateAccessToken(user);
        }
    }

    @Test
    void login_ShouldThrowException_WhenUserNotFound() {
        // Given
        when(userRepository.findByEmail(loginRequestDto.getEmail())).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
            ResourceNotFoundException.class,
            () -> authService.login(loginRequestDto)
        );

        assertEquals("User not found with email: " + loginRequestDto.getEmail(), exception.getMessage());
        verify(userRepository).findByEmail(loginRequestDto.getEmail());
        verify(jwtService, never()).generateAccessToken(any());
    }

    @Test
    void login_ShouldThrowException_WhenInvalidPassword() {
        // Given
        when(userRepository.findByEmail(loginRequestDto.getEmail())).thenReturn(Optional.of(user));

        try (MockedStatic<PasswordUtil> passwordUtilMock = mockStatic(PasswordUtil.class)) {
            passwordUtilMock.when(() -> PasswordUtil.checkPassword(loginRequestDto.getPassword(), user.getPassword()))
                .thenReturn(false);

            // When & Then
            BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> authService.login(loginRequestDto)
            );

            assertEquals("Incorrect password", exception.getMessage());
            verify(userRepository).findByEmail(loginRequestDto.getEmail());
            verify(jwtService, never()).generateAccessToken(any());
        }
    }

    @Test
    void signUp_ShouldHandleNullInputGracefully() {
        // When & Then
        assertThrows(NullPointerException.class, () -> authService.signUp(null));
    }

    @Test
    void login_ShouldHandleNullInputGracefully() {
        // When & Then
        assertThrows(NullPointerException.class, () -> authService.login(null));
    }
}
