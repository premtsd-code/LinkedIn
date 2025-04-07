package com.premtsd.linkedin.postservice.service;

import com.premtsd.linkedin.postservice.auth.UserContextHolder;
import com.premtsd.linkedin.postservice.clients.ConnectionsClient;
import com.premtsd.linkedin.postservice.dto.PersonDto;
import com.premtsd.linkedin.postservice.dto.PostCreateRequestDto;
import com.premtsd.linkedin.postservice.dto.PostDto;
import com.premtsd.linkedin.postservice.entity.Post;
import com.premtsd.linkedin.postservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.postservice.repository.PostsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
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

    public PostDto createPost(PostCreateRequestDto postDto, Long userId) {
        Post post = modelMapper.map(postDto, Post.class);
        post.setUserId(userId);

        Post savedPost = postsRepository.save(post);
        return modelMapper.map(savedPost, PostDto.class);
    }

    public PostDto getPostById(Long postId) {
        log.debug("Retrieving post with ID: {}", postId);

        Long userId = UserContextHolder.getCurrentUserId();

        List<PersonDto> firstConnections = connectionsClient.getFirstConnections();

//        TODO send Notifications to all connections

        Post post = postsRepository.findById(postId).orElseThrow(() ->
                new ResourceNotFoundException("Post not found with id: "+postId));
        return modelMapper.map(post, PostDto.class);
    }

    public List<PostDto> getAllPostsOfUser(Long userId) {
        List<Post> posts = postsRepository.findByUserId(userId);

        return posts
            .stream()
            .map((element) -> modelMapper.map(element, PostDto.class))
            .collect(Collectors.toList());
    }
}
