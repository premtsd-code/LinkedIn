package com.premtsd.linkedin.postservice.controller;

import com.premtsd.linkedin.postservice.auth.UserContextHolder;
import com.premtsd.linkedin.postservice.dto.PostCreateRequestDto;
import com.premtsd.linkedin.postservice.dto.PostDto;
import com.premtsd.linkedin.postservice.service.PostsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
@Slf4j
public class PostsController {

    private final PostsService postsService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostDto> createPost(
            @RequestPart("content") String content,
            @RequestPart("file") MultipartFile file) {

        log.info("Received create post request with content length: {}", content.length());
        PostCreateRequestDto postDto = new PostCreateRequestDto();
        postDto.setContent(content);
        postDto.setFile(file);

        PostDto createdPost = postsService.createPost(postDto);
        log.info("Post created successfully with ID: {}", createdPost.getId());
        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long postId) {
        log.info("Received request to get post with ID: {}", postId);
        PostDto postDto = postsService.getPostById(postId);
        log.debug("Post retrieved successfully: {}", postId);
        return ResponseEntity.ok(postDto);
    }

    @GetMapping("/users/{userId}/allPosts")
    public List<PostDto> getAllPostsOfUser(@PathVariable Long userId) {
        log.info("Received request to get all posts for user: {}", userId);
        List<PostDto> posts = postsService.getAllPostsOfUser(userId);
        log.info("Retrieved {} posts for user: {}", posts.size(), userId);
        return posts;
    }

}
