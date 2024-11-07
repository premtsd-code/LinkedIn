package com.premtsd.linkedin.userservice.dto;

import com.premtsd.linkedin.userservice.entity.Role;
import lombok.Data;

import java.util.Set;

@Data
public class SignupRequestDto {
    private String name;
    private String email;
    private String password;
    private Set<String> roles;
}
