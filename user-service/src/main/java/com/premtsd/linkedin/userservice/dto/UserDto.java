package com.premtsd.linkedin.userservice.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserDto {
    private Long id;
    private String name;
    private String email;
    private List<String> roles;
}
