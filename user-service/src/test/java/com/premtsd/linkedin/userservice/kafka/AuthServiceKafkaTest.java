package com.premtsd.linkedin.userservice.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.userservice.dto.SignupRequestDto;
import com.premtsd.linkedin.userservice.entity.Role;
import com.premtsd.linkedin.userservice.entity.User;
import com.premtsd.linkedin.userservice.event.UserCreatedEmailEvent;
import com.premtsd.linkedin.userservice.event.UserCreatedEvent;
import com.premtsd.linkedin.userservice.repository.RoleRepository;
import com.premtsd.linkedin.userservice.repository.UserRepository;
import com.premtsd.linkedin.userservice.service.AuthService;
import com.premtsd.linkedin.userservice.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Constructor;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceKafkaTest {

    @Mock
    private KafkaTemplate<Long, UserCreatedEmailEvent> kafkaTemplate;

    @Mock
    private KafkaTemplate<Long, UserCreatedEvent> kafkaTemplate1;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private JwtService jwtService;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<UserCreatedEmailEvent> emailEventCaptor;

    @Captor
    private ArgumentCaptor<UserCreatedEvent> userCreatedEventCaptor;

    @Captor
    private ArgumentCaptor<String> topicCaptor;

    private AuthService authService;

    private SignupRequestDto signupRequestDto;
    private Role userRole;
    private User mappedUser;
    private User savedUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService(
                kafkaTemplate,
                kafkaTemplate1,
                userRepository,
                modelMapper,
                jwtService,
                roleRepository,
                objectMapper
        );

        signupRequestDto = new SignupRequestDto();
        signupRequestDto.setName("John Doe");
        signupRequestDto.setEmail("john@example.com");
        signupRequestDto.setPassword("password123");
        signupRequestDto.setRoles(Set.of("USER"));

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        mappedUser = new User();
        mappedUser.setName("John Doe");
        mappedUser.setEmail("john@example.com");

        savedUser = new User();
        savedUser.setId(1L);
        savedUser.setName("John Doe");
        savedUser.setEmail("john@example.com");
        savedUser.setPassword("hashedPassword");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        savedUser.setRoles(roles);
    }

    private void setupCommonMocks() {
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(userRole));
        when(modelMapper.map(signupRequestDto, User.class)).thenReturn(mappedUser);
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        doReturn(new CompletableFuture<>()).when(kafkaTemplate).send(anyString(), any());
        doReturn(new CompletableFuture<>()).when(kafkaTemplate1).send(anyString(), any());
    }

    @Test
    void signUp_ShouldPublishUserCreatedEmailEvent() {
        setupCommonMocks();

        authService.signUp(signupRequestDto);

        verify(kafkaTemplate).send(eq("userCreatedTopic"), emailEventCaptor.capture());
        UserCreatedEmailEvent capturedEvent = emailEventCaptor.getValue();

        assertEquals("john@example.com", capturedEvent.getTo());
        assertEquals("Your account has been created at LinkedIn-Like", capturedEvent.getSubject());
        assertEquals("Hi John Doe,\n Thanks for signing up", capturedEvent.getBody());
    }

    @Test
    void signUp_ShouldPublishUserCreatedEvent() {
        setupCommonMocks();

        authService.signUp(signupRequestDto);

        verify(kafkaTemplate1).send(eq("user-created-topic"), userCreatedEventCaptor.capture());
        UserCreatedEvent capturedEvent = userCreatedEventCaptor.getValue();

        assertEquals(1L, capturedEvent.getUserId());
        assertEquals("John Doe", capturedEvent.getName());
    }

    @Test
    void signUp_ShouldPublishBothEventsWithCorrectData() {
        setupCommonMocks();

        authService.signUp(signupRequestDto);

        verify(kafkaTemplate).send(eq("userCreatedTopic"), emailEventCaptor.capture());
        verify(kafkaTemplate1).send(eq("user-created-topic"), userCreatedEventCaptor.capture());

        UserCreatedEmailEvent emailEvent = emailEventCaptor.getValue();
        assertNotNull(emailEvent);
        assertEquals("john@example.com", emailEvent.getTo());
        assertEquals("Your account has been created at LinkedIn-Like", emailEvent.getSubject());
        assertEquals("Hi John Doe,\n Thanks for signing up", emailEvent.getBody());

        UserCreatedEvent userEvent = userCreatedEventCaptor.getValue();
        assertNotNull(userEvent);
        assertEquals(1L, userEvent.getUserId());
        assertEquals("John Doe", userEvent.getName());
    }
}
