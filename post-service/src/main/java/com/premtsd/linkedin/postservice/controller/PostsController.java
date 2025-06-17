package com.premtsd.linkedin.postservice.controller;

import com.premtsd.linkedin.postservice.auth.UserContextHolder;
import com.premtsd.linkedin.postservice.dto.PostCreateRequestDto;
import com.premtsd.linkedin.postservice.dto.PostDto;
import com.premtsd.linkedin.postservice.service.PostsService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/core")
@RequiredArgsConstructor
public class PostsController {

    private final PostsService postsService;

    @PostMapping
    public ResponseEntity<PostDto> createPost(@RequestBody PostCreateRequestDto postDto) {
        PostDto createdPost = postsService.createPost(postDto);
        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDto> getPost(@PathVariable Long postId) {
        PostDto postDto = postsService.getPostById(postId);
        return ResponseEntity.ok(postDto);
    }

    @GetMapping("/users/{userId}/allPosts")
    public List<PostDto> getAllPostsOfUser(@PathVariable Long userId) {
        System.out.println("check2");
        List<PostDto> posts = postsService.getAllPostsOfUser(userId);
        System.out.println("check3");
        return posts;
    }

}
