package com.premtsd.linkedin.postservice.event;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PostLikedEvent {
    Long postId;
    Long creatorId;
    Long likedByUserId;
}
