package com.premtsd.linkedin.userservice.event;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreatedEvent {
    private Long userId;
    private String name;
}