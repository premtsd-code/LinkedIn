package com.premtsd.linkedin.postservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.premtsd.linkedin.postservice.dto.PostCreateRequestDto;
import com.premtsd.linkedin.postservice.dto.PostDto;
import com.premtsd.linkedin.postservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.postservice.service.PostsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PostsController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false",
    "eureka.client.enabled=false"
})
class PostsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostsService postsService;

    @Autowired
    private ObjectMapper objectMapper;

    private PostDto testPostDto;
    private PostCreateRequestDto testCreateRequest;

    @BeforeEach
    void setUp() {
        testPostDto = new PostDto();
        testPostDto.setId(1L);
        testPostDto.setContent("Test post content");
        testPostDto.setUserId(100L);
        // Note: PostDto doesn't have setCreatedAt method
        testPostDto.setImageUrl("https://example.com/image.jpg");

        testCreateRequest = new PostCreateRequestDto();
        testCreateRequest.setContent("Test post content");
    }

    @Test
    void createPost_ShouldReturnCreatedPost_WhenValidInput() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test content".getBytes());
        
        when(postsService.createPost(any(PostCreateRequestDto.class))).thenReturn(testPostDto);

        // When & Then
        mockMvc.perform(multipart("/posts")
                .file(file)
                .param("content", "Test post content")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testPostDto.getId()))
                .andExpect(jsonPath("$.content").value(testPostDto.getContent()))
                .andExpect(jsonPath("$.userId").value(testPostDto.getUserId()))
                .andExpect(jsonPath("$.imageUrl").value(testPostDto.getImageUrl()));

        verify(postsService).createPost(any(PostCreateRequestDto.class));
    }

    @Test
    void createPost_ShouldReturnCreatedPost_WhenNoFileProvided() throws Exception {
        // Given
        when(postsService.createPost(any(PostCreateRequestDto.class))).thenReturn(testPostDto);

        // When & Then
        mockMvc.perform(multipart("/posts")
                .param("content", "Test post content")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testPostDto.getId()))
                .andExpect(jsonPath("$.content").value(testPostDto.getContent()));

        verify(postsService).createPost(any(PostCreateRequestDto.class));
    }

    @Test
    void createPost_ShouldReturnBadRequest_WhenContentIsMissing() throws Exception {
        // Given
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test content".getBytes());

        // When & Then
        mockMvc.perform(multipart("/posts")
                .file(file)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(postsService, never()).createPost(any());
    }

    @Test
    void createPost_ShouldReturnBadRequest_WhenContentIsEmpty() throws Exception {
        // When & Then
        mockMvc.perform(multipart("/posts")
                .param("content", "")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest());

        verify(postsService, never()).createPost(any());
    }

    @Test
    void createPost_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Given
        when(postsService.createPost(any(PostCreateRequestDto.class)))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(multipart("/posts")
                .param("content", "Test post content")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError());

        verify(postsService).createPost(any(PostCreateRequestDto.class));
    }

    @Test
    void getPostById_ShouldReturnPost_WhenPostExists() throws Exception {
        // Given
        Long postId = 1L;
        when(postsService.getPostById(postId)).thenReturn(testPostDto);

        // When & Then
        mockMvc.perform(get("/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testPostDto.getId()))
                .andExpect(jsonPath("$.content").value(testPostDto.getContent()))
                .andExpect(jsonPath("$.userId").value(testPostDto.getUserId()));

        verify(postsService).getPostById(postId);
    }

    @Test
    void getPostById_ShouldReturnNotFound_WhenPostDoesNotExist() throws Exception {
        // Given
        Long postId = 999L;
        when(postsService.getPostById(postId))
                .thenThrow(new ResourceNotFoundException("Post not found with id: " + postId));

        // When & Then
        mockMvc.perform(get("/posts/{postId}", postId))
                .andExpect(status().isNotFound());

        verify(postsService).getPostById(postId);
    }

    @Test
    void getPostById_ShouldReturnBadRequest_WhenInvalidPostId() throws Exception {
        // When & Then
        mockMvc.perform(get("/posts/{postId}", "invalid"))
                .andExpect(status().isBadRequest());

        verify(postsService, never()).getPostById(anyLong());
    }

    @Test
    void getAllPostsOfUser_ShouldReturnUserPosts_WhenUserHasPosts() throws Exception {
        // Given
        Long userId = 100L;
        PostDto post2 = new PostDto();
        post2.setId(2L);
        post2.setContent("Second post");
        post2.setUserId(userId);
        
        List<PostDto> posts = Arrays.asList(testPostDto, post2);
        when(postsService.getAllPostsOfUser(userId)).thenReturn(posts);

        // When & Then
        mockMvc.perform(get("/posts/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(testPostDto.getId()))
                .andExpect(jsonPath("$[0].content").value(testPostDto.getContent()))
                .andExpect(jsonPath("$[1].id").value(post2.getId()))
                .andExpect(jsonPath("$[1].content").value(post2.getContent()));

        verify(postsService).getAllPostsOfUser(userId);
    }

    @Test
    void getAllPostsOfUser_ShouldReturnEmptyList_WhenUserHasNoPosts() throws Exception {
        // Given
        Long userId = 100L;
        when(postsService.getAllPostsOfUser(userId)).thenReturn(Arrays.asList());

        // When & Then
        mockMvc.perform(get("/posts/users/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        verify(postsService).getAllPostsOfUser(userId);
    }

    @Test
    void getAllPostsOfUser_ShouldReturnBadRequest_WhenInvalidUserId() throws Exception {
        // When & Then
        mockMvc.perform(get("/posts/users/{userId}", "invalid"))
                .andExpect(status().isBadRequest());

        verify(postsService, never()).getAllPostsOfUser(anyLong());
    }

    @Test
    void getAllPostsOfUser_ShouldReturnInternalServerError_WhenServiceThrowsException() throws Exception {
        // Given
        Long userId = 100L;
        when(postsService.getAllPostsOfUser(userId))
                .thenThrow(new RuntimeException("Service error"));

        // When & Then
        mockMvc.perform(get("/posts/users/{userId}", userId))
                .andExpect(status().isInternalServerError());

        verify(postsService).getAllPostsOfUser(userId);
    }

    @Test
    void createPost_ShouldHandleLargeFile() throws Exception {
        // Given
        byte[] largeContent = new byte[1024 * 1024]; // 1MB
        MockMultipartFile largeFile = new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", largeContent);
        
        when(postsService.createPost(any(PostCreateRequestDto.class))).thenReturn(testPostDto);

        // When & Then
        mockMvc.perform(multipart("/posts")
                .file(largeFile)
                .param("content", "Test post with large image")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testPostDto.getId()));

        verify(postsService).createPost(any(PostCreateRequestDto.class));
    }

    @Test
    void createPost_ShouldHandleDifferentImageFormats() throws Exception {
        // Given
        MockMultipartFile pngFile = new MockMultipartFile(
                "file", "test.png", "image/png", "png content".getBytes());
        
        when(postsService.createPost(any(PostCreateRequestDto.class))).thenReturn(testPostDto);

        // When & Then
        mockMvc.perform(multipart("/posts")
                .file(pngFile)
                .param("content", "Test post with PNG image")
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testPostDto.getId()));

        verify(postsService).createPost(any(PostCreateRequestDto.class));
    }

    @Test
    void createPost_ShouldHandleSpecialCharactersInContent() throws Exception {
        // Given
        String specialContent = "Test post with special chars: @#$%^&*()_+{}|:<>?[]\\;'\",./ and emojis 😀🎉";
        when(postsService.createPost(any(PostCreateRequestDto.class))).thenReturn(testPostDto);

        // When & Then
        mockMvc.perform(multipart("/posts")
                .param("content", specialContent)
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(testPostDto.getId()));

        verify(postsService).createPost(any(PostCreateRequestDto.class));
    }
}
