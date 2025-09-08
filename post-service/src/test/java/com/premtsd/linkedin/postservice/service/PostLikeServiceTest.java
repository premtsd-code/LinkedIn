package com.premtsd.linkedin.postservice.service;

import com.premtsd.linkedin.postservice.auth.UserContextHolder;
import com.premtsd.linkedin.postservice.entity.Post;
import com.premtsd.linkedin.postservice.entity.PostLike;
import com.premtsd.linkedin.postservice.event.PostLikedEvent;
import com.premtsd.linkedin.postservice.exception.BadRequestException;
import com.premtsd.linkedin.postservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.postservice.repository.PostLikeRepository;
import com.premtsd.linkedin.postservice.repository.PostsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PostLikeServiceTest {

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private KafkaTemplate<Long, PostLikedEvent> kafkaTemplate;

    private PostLikeService postLikeService;

    @BeforeEach
    void setUp() {
        postLikeService = new PostLikeService(postLikeRepository, postsRepository, kafkaTemplate);
    }

    @Test
    void likePost_ShouldCreateLike_WhenPostExistsAndNotAlreadyLiked() {
        // Given
        Long postId = 100L;
        Long userId = 200L;
        Post mockPost = new Post();
        mockPost.setId(postId);
        mockPost.setUserId(300L);

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(postsRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            when(postLikeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);

            // When
            postLikeService.likePost(postId);

            // Then
            verify(postsRepository).findById(postId);
            verify(postLikeRepository).existsByUserIdAndPostId(userId, postId);
            verify(postLikeRepository).save(any(PostLike.class));
            verify(kafkaTemplate).send(eq("post-liked-topic"), eq(postId), any(PostLikedEvent.class));
        }
    }

    @Test
    void likePost_ShouldThrowException_WhenPostDoesNotExist() {
        // Given
        Long postId = 999L;
        Long userId = 200L;

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(postsRepository.findById(postId)).thenReturn(Optional.empty());

            // When & Then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> postLikeService.likePost(postId)
            );
            
            assertEquals("Post not found with id: " + postId, exception.getMessage());
            verify(postsRepository).findById(postId);
            verify(postLikeRepository, never()).save(any());
        }
    }

    @Test
    void likePost_ShouldThrowException_WhenPostAlreadyLiked() {
        // Given
        Long postId = 100L;
        Long userId = 200L;
        Post mockPost = new Post();
        mockPost.setId(postId);
        mockPost.setUserId(300L);

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(postsRepository.findById(postId)).thenReturn(Optional.of(mockPost));
            when(postLikeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);

            // When & Then
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> postLikeService.likePost(postId)
            );
            
            assertEquals("Cannot like the same post again.", exception.getMessage());
            verify(postsRepository).findById(postId);
            verify(postLikeRepository).existsByUserIdAndPostId(userId, postId);
            verify(postLikeRepository, never()).save(any());
        }
    }

    @Test
    void unlikePost_ShouldRemoveLike_WhenPostIsLiked() {
        // Given
        Long postId = 100L;
        Long userId = 200L;

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(postsRepository.existsById(postId)).thenReturn(true);
            when(postLikeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(true);

            // When
            postLikeService.unlikePost(postId);

            // Then
            verify(postsRepository).existsById(postId);
            verify(postLikeRepository).existsByUserIdAndPostId(userId, postId);
            verify(postLikeRepository).deleteByUserIdAndPostId(userId, postId);
        }
    }

    @Test
    void unlikePost_ShouldThrowException_WhenPostDoesNotExist() {
        // Given
        Long postId = 999L;
        Long userId = 200L;

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(postsRepository.existsById(postId)).thenReturn(false);

            // When & Then
            ResourceNotFoundException exception = assertThrows(
                    ResourceNotFoundException.class,
                    () -> postLikeService.unlikePost(postId)
            );
            
            assertEquals("Post not found with id: " + postId, exception.getMessage());
            verify(postsRepository).existsById(postId);
            verify(postLikeRepository, never()).deleteByUserIdAndPostId(anyLong(), anyLong());
        }
    }

    @Test
    void unlikePost_ShouldThrowException_WhenPostNotLiked() {
        // Given
        Long postId = 100L;
        Long userId = 200L;

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(postsRepository.existsById(postId)).thenReturn(true);
            when(postLikeRepository.existsByUserIdAndPostId(userId, postId)).thenReturn(false);

            // When & Then
            BadRequestException exception = assertThrows(
                    BadRequestException.class,
                    () -> postLikeService.unlikePost(postId)
            );
            
            assertEquals("Cannot unlike the post which is not liked.", exception.getMessage());
            verify(postsRepository).existsById(postId);
            verify(postLikeRepository).existsByUserIdAndPostId(userId, postId);
            verify(postLikeRepository, never()).deleteByUserIdAndPostId(anyLong(), anyLong());
        }
    }
}
