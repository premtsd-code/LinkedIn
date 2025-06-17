package com.premtsd.linkedin.postservice.event;

import lombok.Data;

@Data
public class PostCreatedEvent {
    Long creatorId;
    String content;
    Long postId;
}
