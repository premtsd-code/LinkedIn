package com.premtsd.linkedin.postservice.dto;

import jakarta.persistence.Column;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class PostDto implements Serializable {
    private Long id;
    private String content;
    private Long userId;
//    private LocalDateTime createdAt;
    private String imageUrl;
}
