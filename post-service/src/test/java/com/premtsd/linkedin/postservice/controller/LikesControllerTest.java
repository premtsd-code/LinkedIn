package com.premtsd.linkedin.postservice.controller;

import com.premtsd.linkedin.postservice.exception.BadRequestException;
import com.premtsd.linkedin.postservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.postservice.service.PostLikeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LikesController.class)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.enabled=false",
    "spring.cloud.config.import-check.enabled=false",
    "eureka.client.enabled=false"
})
class LikesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostLikeService postLikeService;

    @Test
    void likePost_ShouldReturnNoContent_WhenPostLikedSuccessfully() throws Exception {
        // Given
        Long postId = 1L;
        doNothing().when(postLikeService).likePost(postId);

        // When & Then
        mockMvc.perform(post("/likes/{postId}", postId))
                .andExpect(status().isNoContent());

        verify(postLikeService).likePost(postId);
    }

    @Test
    void likePost_ShouldReturnNotFound_WhenPostDoesNotExist() throws Exception {
        // Given
        Long postId = 999L;
        doThrow(new ResourceNotFoundException("Post not found with id: " + postId))
                .when(postLikeService).likePost(postId);

        // When & Then
        mockMvc.perform(post("/likes/{postId}", postId))
                .andExpect(status().isNotFound());

        verify(postLikeService).likePost(postId);
    }

    @Test
    void likePost_ShouldReturnBadRequest_WhenPostAlreadyLiked() throws Exception {
        // Given
        Long postId = 1L;
        doThrow(new BadRequestException("Cannot like the same post again."))
                .when(postLikeService).likePost(postId);

        // When & Then
        mockMvc.perform(post("/likes/{postId}", postId))
                .andExpect(status().isBadRequest());

        verify(postLikeService).likePost(postId);
    }

    @Test
    void unlikePost_ShouldReturnNoContent_WhenPostUnlikedSuccessfully() throws Exception {
        // Given
        Long postId = 1L;
        doNothing().when(postLikeService).unlikePost(postId);

        // When & Then
        mockMvc.perform(delete("/likes/{postId}", postId))
                .andExpect(status().isNoContent());

        verify(postLikeService).unlikePost(postId);
    }

    @Test
    void unlikePost_ShouldReturnNotFound_WhenPostDoesNotExist() throws Exception {
        // Given
        Long postId = 999L;
        doThrow(new ResourceNotFoundException("Post not found with id: " + postId))
                .when(postLikeService).unlikePost(postId);

        // When & Then
        mockMvc.perform(delete("/likes/{postId}", postId))
                .andExpect(status().isNotFound());

        verify(postLikeService).unlikePost(postId);
    }

    @Test
    void unlikePost_ShouldReturnBadRequest_WhenPostNotLiked() throws Exception {
        // Given
        Long postId = 1L;
        doThrow(new BadRequestException("Cannot unlike the post which is not liked."))
                .when(postLikeService).unlikePost(postId);

        // When & Then
        mockMvc.perform(delete("/likes/{postId}", postId))
                .andExpect(status().isBadRequest());

        verify(postLikeService).unlikePost(postId);
    }

    @Test
    void likePost_ShouldReturnBadRequest_WhenInvalidPostId() throws Exception {
        // When & Then
        mockMvc.perform(post("/likes/{postId}", "invalid"))
                .andExpect(status().isBadRequest());

        verify(postLikeService, never()).likePost(anyLong());
    }

    @Test
    void unlikePost_ShouldReturnBadRequest_WhenInvalidPostId() throws Exception {
        // When & Then
        mockMvc.perform(delete("/likes/{postId}", "invalid"))
                .andExpect(status().isBadRequest());

        verify(postLikeService, never()).unlikePost(anyLong());
    }
}
