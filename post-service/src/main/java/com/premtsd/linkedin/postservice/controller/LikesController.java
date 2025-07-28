package com.premtsd.linkedin.postservice.controller;

import com.premtsd.linkedin.postservice.service.PostLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/likes")
@RequiredArgsConstructor
@Slf4j
public class LikesController {

    private final PostLikeService postLikeService;

    @PostMapping("/{postId}")
    public ResponseEntity<Void> likePost(@PathVariable Long postId) {
        log.info("Received request to like post: {}", postId);
        postLikeService.likePost(postId);
        log.info("Post liked successfully: {}", postId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> unlikePost(@PathVariable Long postId) {
        log.info("Received request to unlike post: {}", postId);
        postLikeService.unlikePost(postId);
        log.info("Post unliked successfully: {}", postId);
        return ResponseEntity.noContent().build();
    }

}
