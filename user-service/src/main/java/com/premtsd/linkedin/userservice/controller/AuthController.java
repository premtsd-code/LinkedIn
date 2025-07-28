package com.premtsd.linkedin.userservice.controller;

import com.premtsd.linkedin.userservice.dto.LoginRequestDto;
import com.premtsd.linkedin.userservice.dto.SignupRequestDto;
import com.premtsd.linkedin.userservice.dto.UserDto;
import com.premtsd.linkedin.userservice.dto.UserLoginDto;
import com.premtsd.linkedin.userservice.entity.User;
import com.premtsd.linkedin.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<UserDto> signUp(@RequestBody SignupRequestDto signupRequestDto) {
        log.info("Received signup request for email: {}", signupRequestDto.getEmail());
        UserDto userDto = authService.signUp(signupRequestDto);
        log.info("User signup successful for email: {}", signupRequestDto.getEmail());
        return new ResponseEntity<>(userDto, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        log.info("Received login request for email: {}", loginRequestDto.getEmail());
        UserLoginDto userLoginDto = authService.login(loginRequestDto);
        log.info("User login successful for email: {}", loginRequestDto.getEmail());
        return ResponseEntity.ok(userLoginDto);
    }
}
