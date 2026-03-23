package com.premtsd.linkedin.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.userservice.dto.LoginRequestDto;
import com.premtsd.linkedin.userservice.dto.SignupRequestDto;
import com.premtsd.linkedin.userservice.dto.UserDto;
import com.premtsd.linkedin.userservice.dto.UserLoginDto;
import com.premtsd.linkedin.userservice.exception.BadRequestException;
import com.premtsd.linkedin.userservice.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.config.import=optional:configserver:",
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false"
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void shouldReturnCreatedWithUserDtoWhenSignupIsSuccessful() throws Exception {
        // Given
        SignupRequestDto signupRequest = new SignupRequestDto();
        signupRequest.setName("John Doe");
        signupRequest.setEmail("john@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRoles(Set.of("ROLE_USER"));

        UserDto userDto = new UserDto();
        userDto.setId(1L);
        userDto.setName("John Doe");
        userDto.setEmail("john@example.com");
        userDto.setRoles(List.of("ROLE_USER"));

        when(authService.signUp(any(SignupRequestDto.class))).thenReturn(userDto);

        // When & Then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_USER"));

        verify(authService).signUp(any(SignupRequestDto.class));
    }

    @Test
    void shouldReturnOkWithUserLoginDtoWhenLoginIsSuccessful() throws Exception {
        // Given
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("password123");

        UserLoginDto userLoginDto = new UserLoginDto();
        userLoginDto.setId(1L);
        userLoginDto.setName("John Doe");
        userLoginDto.setEmail("john@example.com");
        userLoginDto.setRoles(List.of("ROLE_USER"));
        userLoginDto.setToken("jwt-token-value");

        when(authService.login(any(LoginRequestDto.class))).thenReturn(userLoginDto);

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token-value"));

        verify(authService).login(any(LoginRequestDto.class));
    }

    @Test
    void shouldReturnBadRequestWhenSignupWithDuplicateEmail() throws Exception {
        // Given
        SignupRequestDto signupRequest = new SignupRequestDto();
        signupRequest.setName("John Doe");
        signupRequest.setEmail("existing@example.com");
        signupRequest.setPassword("password123");
        signupRequest.setRoles(Set.of("ROLE_USER"));

        when(authService.signUp(any(SignupRequestDto.class)))
                .thenThrow(new BadRequestException("User with email existing@example.com already exists"));

        // When & Then
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signupRequest)))
                .andExpect(status().isBadRequest());

        verify(authService).signUp(any(SignupRequestDto.class));
    }

    @Test
    void shouldReturnBadRequestWhenLoginWithWrongPassword() throws Exception {
        // Given
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("john@example.com");
        loginRequest.setPassword("wrong-password");

        when(authService.login(any(LoginRequestDto.class)))
                .thenThrow(new BadRequestException("Invalid credentials"));

        // When & Then
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());

        verify(authService).login(any(LoginRequestDto.class));
    }
}
