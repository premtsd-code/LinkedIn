package com.premtsd.linkedin.userservice.dto;

import lombok.Data;

@Data
public class UserLoginDto extends UserDto{
    private String token;
}
