package com.premtsd.linkedin.postservice.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PostCreateRequestDto {
    private String content;
    private MultipartFile file;
}
