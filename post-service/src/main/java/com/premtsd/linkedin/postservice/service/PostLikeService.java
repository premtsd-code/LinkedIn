package com.premtsd.linkedin.postservice.service;

import com.premtsd.linkedin.postservice.auth.UserContextHolder;
import com.premtsd.linkedin.postservice.entity.Post;
import com.premtsd.linkedin.postservice.entity.PostLike;
import com.premtsd.linkedin.postservice.event.PostLikedEvent;
import com.premtsd.linkedin.postservice.exception.BadRequestException;
import com.premtsd.linkedin.postservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.postservice.repository.PostLikeRepository;
import com.premtsd.linkedin.postservice.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostsRepository postsRepository;
    private final KafkaTemplate<Long, PostLikedEvent> kafkaTemplate;

    public void likePost(Long postId) {
        Long userId = UserContextHolder.getCurrentUserId();
        log.info("User {} attempting to like post: {}", userId, postId);

        Post post = postsRepository.findById(postId).orElseThrow(
                () -> {
                    log.warn("Post not found for like operation: {}", postId);
                    return new ResourceNotFoundException("Post not found with id: "+postId);
                });

        boolean alreadyLiked = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        if(alreadyLiked) {
            log.warn("User {} attempted to like already liked post: {}", userId, postId);
            throw new BadRequestException("Cannot like the same post again.");
        }

        log.debug("Creating post like record");
        PostLike postLike = new PostLike();
        postLike.setPostId(postId);
        postLike.setUserId(userId);
        postLikeRepository.save(postLike);
        log.info("Post {} liked successfully by user {}", postId, userId);

        log.debug("Publishing post liked event");
        PostLikedEvent postLikedEvent = PostLikedEvent.builder()
                .postId(postId)
                .likedByUserId(userId)
                .creatorId(post.getUserId()).build();

        kafkaTemplate.send("post-liked-topic", postId, postLikedEvent);
        log.debug("Post liked event published for post: {}", postId);
    }

    public void unlikePost(Long postId) {
        Long userId = UserContextHolder.getCurrentUserId();
        log.info("User {} attempting to unlike post: {}", userId, postId);

        boolean exists = postsRepository.existsById(postId);
        if(!exists) {
            log.warn("Post not found for unlike operation: {}", postId);
            throw new ResourceNotFoundException("Post not found with id: "+postId);
        }

        boolean alreadyLiked = postLikeRepository.existsByUserIdAndPostId(userId, postId);
        if(!alreadyLiked) {
            log.warn("User {} attempted to unlike non-liked post: {}", userId, postId);
            throw new BadRequestException("Cannot unlike the post which is not liked.");
        }

        log.debug("Removing post like record");
        postLikeRepository.deleteByUserIdAndPostId(userId, postId);
        log.info("Post {} unliked successfully by user {}", postId, userId);
    }
}
