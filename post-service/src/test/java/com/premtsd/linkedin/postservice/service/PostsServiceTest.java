package com.premtsd.linkedin.postservice.service;

import com.premtsd.linkedin.postservice.auth.UserContextHolder;
import com.premtsd.linkedin.postservice.clients.ConnectionsClient;
import com.premtsd.linkedin.postservice.clients.UploaderClient;
import com.premtsd.linkedin.postservice.dto.PostCreateRequestDto;
import com.premtsd.linkedin.postservice.dto.PostDto;
import com.premtsd.linkedin.postservice.entity.Post;
import com.premtsd.linkedin.postservice.event.PostCreatedEvent;
import com.premtsd.linkedin.postservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.postservice.repository.PostsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PostsServiceTest {

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ConnectionsClient connectionsClient;

    @Mock
    private UploaderClient uploaderClient;

    @Mock
    private KafkaTemplate<Long, PostCreatedEvent> kafkaTemplate;

    @Mock
    private UploaderServiceWrapper uploaderServiceWrapper;

    private PostsService postsService;

    private Post testPost;
    private PostDto testPostDto;
    private PostCreateRequestDto testCreateRequest;

    @BeforeEach
    void setUp() {
        postsService = new PostsService(
                postsRepository, 
                modelMapper, 
                connectionsClient, 
                uploaderClient, 
                kafkaTemplate, 
                uploaderServiceWrapper
        );

        // Setup test data
        testPost = new Post();
        testPost.setId(1L);
        testPost.setContent("Test post content");
        testPost.setUserId(100L);
        testPost.setCreatedAt(LocalDateTime.now());
        testPost.setImageUrl("https://example.com/image.jpg");

        testPostDto = new PostDto();
        testPostDto.setId(1L);
        testPostDto.setContent("Test post content");
        testPostDto.setUserId(100L);
        testPostDto.setImageUrl("https://example.com/image.jpg");

        testCreateRequest = new PostCreateRequestDto();
        testCreateRequest.setContent("Test post content");
    }

    @Test
    void createPost_ShouldCreatePostSuccessfully_WhenValidInput() {
        // Given
        Long userId = 100L;
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test content".getBytes());
        testCreateRequest.setFile(file);
        String uploadedImageUrl = "https://uploaded.com/image.jpg";

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(modelMapper.map(testCreateRequest, Post.class)).thenReturn(testPost);
            when(uploaderServiceWrapper.uploadFile(file)).thenReturn(uploadedImageUrl);
            when(postsRepository.save(any(Post.class))).thenReturn(testPost);
            when(modelMapper.map(testPost, PostDto.class)).thenReturn(testPostDto);

            // When
            PostDto result = postsService.createPost(testCreateRequest);

            // Then
            assertNotNull(result);
            assertEquals(testPostDto.getId(), result.getId());
            assertEquals(testPostDto.getContent(), result.getContent());
            
            verify(postsRepository).save(any(Post.class));
            verify(uploaderServiceWrapper).uploadFile(file);
            verify(kafkaTemplate).send(eq("post-created-topic"), any(PostCreatedEvent.class));
        }
    }

    @Test
    void createPost_ShouldCreatePostWithoutImage_WhenNoFileProvided() {
        // Given
        Long userId = 100L;
        testCreateRequest.setFile(null);

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(modelMapper.map(testCreateRequest, Post.class)).thenReturn(testPost);
            when(postsRepository.save(any(Post.class))).thenReturn(testPost);
            when(modelMapper.map(testPost, PostDto.class)).thenReturn(testPostDto);

            // When
            PostDto result = postsService.createPost(testCreateRequest);

            // Then
            assertNotNull(result);
            assertEquals(testPostDto.getId(), result.getId());
            
            verify(postsRepository).save(any(Post.class));
            verify(uploaderServiceWrapper, never()).uploadFile(any());
            verify(kafkaTemplate).send(eq("post-created-topic"), any(PostCreatedEvent.class));
        }
    }

    @Test
    void createPost_ShouldPublishKafkaEvent_WhenPostCreated() {
        // Given
        Long userId = 100L;

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(modelMapper.map(testCreateRequest, Post.class)).thenReturn(testPost);
            when(postsRepository.save(any(Post.class))).thenReturn(testPost);
            when(modelMapper.map(testPost, PostDto.class)).thenReturn(testPostDto);

            // When
            postsService.createPost(testCreateRequest);

            // Then
            ArgumentCaptor<PostCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PostCreatedEvent.class);
            verify(kafkaTemplate).send(eq("post-created-topic"), eventCaptor.capture());
            
            PostCreatedEvent capturedEvent = eventCaptor.getValue();
            assertEquals(testPost.getId(), capturedEvent.getPostId());
            assertEquals(userId, capturedEvent.getCreatorId());
            assertEquals(testPost.getContent(), capturedEvent.getContent());
        }
    }

    @Test
    void getPostById_ShouldReturnPost_WhenPostExists() {
        // Given
        Long postId = 1L;
        when(postsRepository.findById(postId)).thenReturn(Optional.of(testPost));
        when(modelMapper.map(testPost, PostDto.class)).thenReturn(testPostDto);

        // When
        PostDto result = postsService.getPostById(postId);

        // Then
        assertNotNull(result);
        assertEquals(testPostDto.getId(), result.getId());
        assertEquals(testPostDto.getContent(), result.getContent());
        
        verify(postsRepository).findById(postId);
        verify(modelMapper).map(testPost, PostDto.class);
    }

    @Test
    void getPostById_ShouldThrowException_WhenPostNotFound() {
        // Given
        Long postId = 999L;
        when(postsRepository.findById(postId)).thenReturn(Optional.empty());

        // When & Then
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> postsService.getPostById(postId)
        );
        
        assertEquals("Post not found with id: " + postId, exception.getMessage());
        verify(postsRepository).findById(postId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void getAllPostsOfUser_ShouldReturnUserPosts_WhenUserHasPosts() {
        // Given
        Long userId = 100L;
        Post post2 = new Post();
        post2.setId(2L);
        post2.setContent("Second post");
        post2.setUserId(userId);
        
        List<Post> posts = Arrays.asList(testPost, post2);
        
        PostDto postDto2 = new PostDto();
        postDto2.setId(2L);
        postDto2.setContent("Second post");
        postDto2.setUserId(userId);

        when(postsRepository.findByUserId(userId)).thenReturn(posts);
        when(modelMapper.map(testPost, PostDto.class)).thenReturn(testPostDto);
        when(modelMapper.map(post2, PostDto.class)).thenReturn(postDto2);

        // When
        List<PostDto> result = postsService.getAllPostsOfUser(userId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(testPostDto.getId(), result.get(0).getId());
        assertEquals(postDto2.getId(), result.get(1).getId());
        
        verify(postsRepository).findByUserId(userId);
        verify(modelMapper, times(2)).map(any(Post.class), eq(PostDto.class));
    }

    @Test
    void getAllPostsOfUser_ShouldReturnEmptyList_WhenUserHasNoPosts() {
        // Given
        Long userId = 100L;
        when(postsRepository.findByUserId(userId)).thenReturn(Arrays.asList());

        // When
        List<PostDto> result = postsService.getAllPostsOfUser(userId);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
        
        verify(postsRepository).findByUserId(userId);
        verify(modelMapper, never()).map(any(), any());
    }

    @Test
    void createPost_ShouldHandleUploadFailure_WhenFileUploadFails() {
        // Given
        Long userId = 100L;
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test content".getBytes());
        testCreateRequest.setFile(file);

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(modelMapper.map(testCreateRequest, Post.class)).thenReturn(testPost);
            when(uploaderServiceWrapper.uploadFile(file)).thenThrow(new RuntimeException("Upload failed"));

            // When & Then
            assertThrows(RuntimeException.class, () -> postsService.createPost(testCreateRequest));
            
            verify(uploaderServiceWrapper).uploadFile(file);
            verify(postsRepository, never()).save(any());
            verify(kafkaTemplate, never()).send(anyString(), any());
        }
    }

    @Test
    void createPost_ShouldSetCorrectUserId_WhenCreatingPost() {
        // Given
        Long userId = 200L;

        try (MockedStatic<UserContextHolder> mockedUserContext = mockStatic(UserContextHolder.class)) {
            mockedUserContext.when(UserContextHolder::getCurrentUserId).thenReturn(userId);
            
            when(modelMapper.map(testCreateRequest, Post.class)).thenReturn(testPost);
            when(postsRepository.save(any(Post.class))).thenReturn(testPost);
            when(modelMapper.map(testPost, PostDto.class)).thenReturn(testPostDto);

            // When
            postsService.createPost(testCreateRequest);

            // Then
            ArgumentCaptor<Post> postCaptor = ArgumentCaptor.forClass(Post.class);
            verify(postsRepository).save(postCaptor.capture());
            
            Post capturedPost = postCaptor.getValue();
            assertEquals(userId, capturedPost.getUserId());
        }
    }
}
