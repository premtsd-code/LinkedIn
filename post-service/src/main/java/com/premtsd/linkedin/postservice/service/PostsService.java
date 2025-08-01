package com.premtsd.linkedin.postservice.service;

import com.premtsd.linkedin.postservice.auth.UserContextHolder;
import com.premtsd.linkedin.postservice.clients.ConnectionsClient;
import com.premtsd.linkedin.postservice.clients.UploaderClient;
import com.premtsd.linkedin.postservice.dto.PersonDto;
import com.premtsd.linkedin.postservice.dto.PostCreateRequestDto;
import com.premtsd.linkedin.postservice.dto.PostDto;
import com.premtsd.linkedin.postservice.entity.Post;
import com.premtsd.linkedin.postservice.event.PostCreatedEvent;
import com.premtsd.linkedin.postservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.postservice.repository.PostsRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
//import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
@Service
@RequiredArgsConstructor
@Slf4j
public class PostsService {

    private final PostsRepository postsRepository;
    private final ModelMapper modelMapper;
    private final ConnectionsClient connectionsClient;
    private final UploaderClient uploaderClient;

    private final KafkaTemplate<Long, PostCreatedEvent> kafkaTemplate;
    private final UploaderServiceWrapper uploaderServiceWrapper;



    public PostDto createPost(PostCreateRequestDto postDto) {
        Long userId = UserContextHolder.getCurrentUserId();
        log.info("Creating post for user: {}", userId);

        Post post = modelMapper.map(postDto, Post.class);
        post.setUserId(userId);

        if (postDto.getFile() != null) {
            log.debug("Uploading file for post");
            String imageUrl = uploaderServiceWrapper.uploadFile(postDto.getFile());
            post.setImageUrl(imageUrl);
            log.debug("File uploaded successfully, URL: {}", imageUrl);
        }

        log.debug("Saving post to database");
        Post savedPost = postsRepository.save(post);
        log.info("Post saved successfully with ID: {}", savedPost.getId());

        log.debug("Publishing post created event");
        PostCreatedEvent postCreatedEvent = PostCreatedEvent.builder()
                .postId(savedPost.getId())
                .creatorId(userId)
                .content(savedPost.getContent())
                .build();

        kafkaTemplate.send("post-created-topic", postCreatedEvent);
        log.info("Post creation process completed for post ID: {}", savedPost.getId());
        return modelMapper.map(savedPost, PostDto.class);
    }



    public PostDto getPostById(Long postId) {
        log.debug("Retrieving post with ID: {}", postId);

        Post post = postsRepository.findById(postId).orElseThrow(() ->
                new ResourceNotFoundException("Post not found with id: "+postId));
        return modelMapper.map(post, PostDto.class);
    }

    @Cacheable(value = "post", key = "#userId")
    public List<PostDto> getAllPostsOfUser(Long userId) {
        log.info("Retrieving all posts for user: {}", userId);
        List<Post> posts = postsRepository.findByUserId(userId);
        log.debug("Found {} posts for user: {}", posts.size(), userId);
        return posts
                .stream()
                .map((element) -> modelMapper.map(element, PostDto.class))
                .collect(Collectors.toList());
    }
}
