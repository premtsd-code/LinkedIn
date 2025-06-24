package com.premtsd.linkedin.userservice.event;


import lombok.Data;

@Data
public class UserCreatedEmailEvent {
    private String to;
    private String subject;
    private String body;
}
