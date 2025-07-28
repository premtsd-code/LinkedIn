package com.premtsd.linkedin.notificationservice.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class NotificationDto {
    private Long id;
    private Long userId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
