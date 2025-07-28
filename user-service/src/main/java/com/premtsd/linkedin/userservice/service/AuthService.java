package com.premtsd.linkedin.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final KafkaTemplate<Long, UserCreatedEmailEvent> kafkaTemplate;
    private final KafkaTemplate<Long, UserCreatedEvent> kafkaTemplate1;
    private final UserRepository userRepository;
    private final ModelMapper modelMapper;
    private final JwtService jwtService;
    private final RoleRepository roleRepository;
    private final ObjectMapper objectMapper;

    public UserDto signUp(SignupRequestDto signupRequestDto) {
        log.info("Processing signup request for email: {}", signupRequestDto.getEmail());

        boolean exists = userRepository.existsByEmail(signupRequestDto.getEmail());
        if(exists) {
            log.warn("Signup attempt for existing email: {}", signupRequestDto.getEmail());
            throw new BadRequestException("User already exists, cannot signup again.");
        }

        log.debug("Processing roles for user: {}", signupRequestDto.getRoles());
        Set<Role> roleSet=new HashSet<>();
        for(String role:signupRequestDto.getRoles()){
            Optional<Role> optionalRole= roleRepository.findByName(role);
            if(!optionalRole.isPresent()){
                if(role.equals("ADMIN") || role.equals("USER")) {
                    log.info("Creating new role: {}", role);
                    Role newRole = new Role();
                    newRole.setName(role);
                    Role savedRole = roleRepository.save(newRole);
                }
                log.error("Invalid role requested: {}", role);
                throw new BadRequestException("Role does not exist. - " + role);
            }
            roleSet.add(optionalRole.get());
        }

        User user = modelMapper.map(signupRequestDto, User.class);
        user.setRoles(roleSet);
        user.setPassword(PasswordUtil.hashPassword(signupRequestDto.getPassword()));

        log.debug("Saving user to database");
        User savedUser = userRepository.save(user);
        log.info("User created successfully with ID: {}", savedUser.getId());

        log.debug("Sending user created email event");
        UserCreatedEmailEvent userCreatedEmailEvent=new UserCreatedEmailEvent();
        userCreatedEmailEvent.setTo(savedUser.getEmail());
        userCreatedEmailEvent.setSubject("Your account has been created at LinkedIn-Like");
        userCreatedEmailEvent.setBody("Hi "+savedUser.getName()+",\n"+" Thanks for signing up");
        kafkaTemplate.send("userCreatedTopic", userCreatedEmailEvent);

        log.debug("Sending user created event");
        UserCreatedEvent userCreatedEvent=new UserCreatedEvent();
        userCreatedEvent.setUserId(savedUser.getId());
        userCreatedEvent.setName(savedUser.getName());
        kafkaTemplate1.send("user-created-topic", userCreatedEvent);

        log.info("Signup process completed for user: {}", savedUser.getEmail());
        return mapUserToUserDto(savedUser);
    }


    public UserLoginDto login(LoginRequestDto loginRequestDto) {
        log.info("Processing login request for email: {}", loginRequestDto.getEmail());

        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login attempt for non-existent email: {}", loginRequestDto.getEmail());
                    return new ResourceNotFoundException("User not found with email: "+loginRequestDto.getEmail());
                });

        log.debug("User found, validating password for user ID: {}", user.getId());
        boolean isPasswordMatch = PasswordUtil.checkPassword(loginRequestDto.getPassword(), user.getPassword());

        if(!isPasswordMatch) {
            log.warn("Invalid password attempt for email: {}", loginRequestDto.getEmail());
            throw new BadRequestException("Incorrect password");
        }

        log.info("Login successful for user: {}", loginRequestDto.getEmail());
        UserLoginDto userLoginDto=mapUserToUserLoginDto(user);
        userLoginDto.setToken(jwtService.generateAccessToken(user));
        return userLoginDto;
    }

    private UserDto mapUserToUserDto(User user) {
        // Convert Set<Role> to List<String> of role names
        List<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())  // Convert Role to String (role name)
                .collect(Collectors.toList());

        // Map the rest of the User properties to UserDto
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        userDto.setRoles(roleNames);

        return userDto;
    }
    private UserLoginDto mapUserToUserLoginDto(User user) {
        // Convert Set<Role> to List<String> of role names
        List<String> roleNames = user.getRoles().stream()
                .map(role -> role.getName())  // Convert Role to String (role name)
                .collect(Collectors.toList());

        // Map the rest of the User properties to UserDto
        UserLoginDto userLoginDto = new UserLoginDto();
        userLoginDto.setId(user.getId());
        userLoginDto.setName(user.getName());
        userLoginDto.setEmail(user.getEmail());
        userLoginDto.setRoles(roleNames);

        return userLoginDto;
    }
}
