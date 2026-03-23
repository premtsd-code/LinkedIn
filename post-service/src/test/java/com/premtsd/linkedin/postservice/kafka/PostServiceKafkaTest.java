package com.premtsd.linkedin.postservice.kafka;

import com.premtsd.linkedin.postservice.auth.UserContextHolder;
import com.premtsd.linkedin.postservice.clients.ConnectionsClient;
import com.premtsd.linkedin.postservice.clients.UploaderClient;
import com.premtsd.linkedin.postservice.dto.PostCreateRequestDto;
import com.premtsd.linkedin.postservice.dto.PostDto;
import com.premtsd.linkedin.postservice.entity.Post;
import com.premtsd.linkedin.postservice.entity.PostLike;
import com.premtsd.linkedin.postservice.event.PostCreatedEvent;
import com.premtsd.linkedin.postservice.event.PostLikedEvent;
import com.premtsd.linkedin.postservice.repository.PostLikeRepository;
import com.premtsd.linkedin.postservice.repository.PostsRepository;
import com.premtsd.linkedin.postservice.service.PostLikeService;
import com.premtsd.linkedin.postservice.service.PostsService;
import com.premtsd.linkedin.postservice.service.UploaderServiceWrapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceKafkaTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class PostCreatedKafkaTest {

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

        private PostsService createService() {
            return new PostsService(postsRepository, modelMapper, connectionsClient,
                    uploaderClient, kafkaTemplate, uploaderServiceWrapper);
        }

        @Test
        void createPost_ShouldPublishPostCreatedEvent() {
            postsService = createService();

            PostCreateRequestDto dto = new PostCreateRequestDto();
            dto.setContent("Test");

            Post post = new Post();
            post.setContent("Test");

            Post savedPost = new Post();
            savedPost.setId(100L);
            savedPost.setUserId(1L);
            savedPost.setContent("Test");

            PostDto postDto = new PostDto();
            postDto.setId(100L);

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(1L);
                when(modelMapper.map(dto, Post.class)).thenReturn(post);
                when(postsRepository.save(post)).thenReturn(savedPost);
                when(modelMapper.map(savedPost, PostDto.class)).thenReturn(postDto);

                postsService.createPost(dto);

                verify(kafkaTemplate).send(eq("post-created-topic"), any(PostCreatedEvent.class));
            }
        }

        @Test
        void createPost_ShouldPublishEventWithCorrectCreatorId() {
            postsService = createService();

            PostCreateRequestDto dto = new PostCreateRequestDto();
            dto.setContent("Test");

            Post post = new Post();
            post.setContent("Test");

            Post savedPost = new Post();
            savedPost.setId(100L);
            savedPost.setUserId(1L);
            savedPost.setContent("Test");

            PostDto postDto = new PostDto();
            postDto.setId(100L);

            ArgumentCaptor<PostCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PostCreatedEvent.class);

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(1L);
                when(modelMapper.map(dto, Post.class)).thenReturn(post);
                when(postsRepository.save(post)).thenReturn(savedPost);
                when(modelMapper.map(savedPost, PostDto.class)).thenReturn(postDto);

                postsService.createPost(dto);

                verify(kafkaTemplate).send(eq("post-created-topic"), eventCaptor.capture());
                PostCreatedEvent capturedEvent = eventCaptor.getValue();
                assertThat(capturedEvent.getCreatorId()).isEqualTo(1L);
            }
        }

        @Test
        void createPost_ShouldPublishEventWithCorrectPostId() {
            postsService = createService();

            PostCreateRequestDto dto = new PostCreateRequestDto();
            dto.setContent("Test");

            Post post = new Post();
            post.setContent("Test");

            Post savedPost = new Post();
            savedPost.setId(200L);
            savedPost.setUserId(1L);
            savedPost.setContent("Test");

            PostDto postDto = new PostDto();
            postDto.setId(200L);

            ArgumentCaptor<PostCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PostCreatedEvent.class);

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(1L);
                when(modelMapper.map(dto, Post.class)).thenReturn(post);
                when(postsRepository.save(post)).thenReturn(savedPost);
                when(modelMapper.map(savedPost, PostDto.class)).thenReturn(postDto);

                postsService.createPost(dto);

                verify(kafkaTemplate).send(eq("post-created-topic"), eventCaptor.capture());
                PostCreatedEvent capturedEvent = eventCaptor.getValue();
                assertThat(capturedEvent.getPostId()).isEqualTo(200L);
            }
        }
    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class PostLikedKafkaTest {

        @Mock
        private PostLikeRepository postLikeRepository;

        @Mock
        private PostsRepository postsRepository;

        @Mock
        private KafkaTemplate<Long, PostLikedEvent> kafkaTemplate;

        @InjectMocks
        private PostLikeService postLikeService;

        @Test
        void likePost_ShouldPublishPostLikedEvent() {
            Long postId = 10L;

            Post post = new Post();
            post.setId(postId);
            post.setUserId(5L);

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(1L);
                when(postsRepository.findById(postId)).thenReturn(Optional.of(post));
                when(postLikeRepository.existsByUserIdAndPostId(1L, postId)).thenReturn(false);

                postLikeService.likePost(postId);

                verify(kafkaTemplate).send(eq("post-liked-topic"), eq(postId), any(PostLikedEvent.class));
            }
        }

        @Test
        void likePost_ShouldPublishEventWithCorrectFields() {
            Long postId = 10L;

            Post post = new Post();
            post.setId(postId);
            post.setUserId(5L);

            ArgumentCaptor<PostLikedEvent> eventCaptor = ArgumentCaptor.forClass(PostLikedEvent.class);

            try (MockedStatic<UserContextHolder> mockedStatic = mockStatic(UserContextHolder.class)) {
                mockedStatic.when(UserContextHolder::getCurrentUserId).thenReturn(1L);
                when(postsRepository.findById(postId)).thenReturn(Optional.of(post));
                when(postLikeRepository.existsByUserIdAndPostId(1L, postId)).thenReturn(false);

                postLikeService.likePost(postId);

                verify(kafkaTemplate).send(eq("post-liked-topic"), eq(postId), eventCaptor.capture());
                PostLikedEvent capturedEvent = eventCaptor.getValue();
                assertThat(capturedEvent.getPostId()).isEqualTo(10L);
                assertThat(capturedEvent.getLikedByUserId()).isEqualTo(1L);
                assertThat(capturedEvent.getCreatorId()).isEqualTo(5L);
            }
        }
    }
}
