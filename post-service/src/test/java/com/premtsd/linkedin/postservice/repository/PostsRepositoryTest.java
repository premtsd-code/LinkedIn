package com.premtsd.linkedin.postservice.repository;

import com.premtsd.linkedin.postservice.entity.Post;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class PostsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private PostsRepository postsRepository;

    private Post testPost1;
    private Post testPost2;
    private Post testPost3;

    @BeforeEach
    void setUp() {
        testPost1 = new Post();
        testPost1.setContent("First test post");
        testPost1.setUserId(100L);
        testPost1.setCreatedAt(LocalDateTime.now().minusDays(2));
        testPost1.setImageUrl("https://example.com/image1.jpg");

        testPost2 = new Post();
        testPost2.setContent("Second test post");
        testPost2.setUserId(100L);
        testPost2.setCreatedAt(LocalDateTime.now().minusDays(1));
        testPost2.setImageUrl(null);

        testPost3 = new Post();
        testPost3.setContent("Third test post by different user");
        testPost3.setUserId(200L);
        testPost3.setCreatedAt(LocalDateTime.now());
        testPost3.setImageUrl("https://example.com/image3.jpg");
    }

    @Test
    void save_ShouldPersistPost_WhenValidPost() {
        // When
        Post savedPost = postsRepository.save(testPost1);

        // Then
        assertNotNull(savedPost.getId());
        assertEquals(testPost1.getContent(), savedPost.getContent());
        assertEquals(testPost1.getUserId(), savedPost.getUserId());
        assertEquals(testPost1.getImageUrl(), savedPost.getImageUrl());
        assertNotNull(savedPost.getCreatedAt());
    }

    @Test
    void findById_ShouldReturnPost_WhenPostExists() {
        // Given
        Post savedPost = entityManager.persistAndFlush(testPost1);

        // When
        Optional<Post> foundPost = postsRepository.findById(savedPost.getId());

        // Then
        assertTrue(foundPost.isPresent());
        assertEquals(savedPost.getId(), foundPost.get().getId());
        assertEquals(testPost1.getContent(), foundPost.get().getContent());
        assertEquals(testPost1.getUserId(), foundPost.get().getUserId());
    }

    @Test
    void findById_ShouldReturnEmpty_WhenPostDoesNotExist() {
        // When
        Optional<Post> foundPost = postsRepository.findById(999L);

        // Then
        assertFalse(foundPost.isPresent());
    }

    @Test
    void findByUserId_ShouldReturnUserPosts_WhenUserHasPosts() {
        // Given
        entityManager.persistAndFlush(testPost1);
        entityManager.persistAndFlush(testPost2);
        entityManager.persistAndFlush(testPost3);

        // When
        List<Post> userPosts = postsRepository.findByUserId(100L);

        // Then
        assertEquals(2, userPosts.size());
        assertTrue(userPosts.stream().allMatch(post -> post.getUserId().equals(100L)));
        
        // Verify posts are ordered by creation date (newest first)
        assertEquals(testPost2.getContent(), userPosts.get(0).getContent());
        assertEquals(testPost1.getContent(), userPosts.get(1).getContent());
    }

    @Test
    void findByUserId_ShouldReturnEmptyList_WhenUserHasNoPosts() {
        // Given
        entityManager.persistAndFlush(testPost1);
        entityManager.persistAndFlush(testPost2);

        // When
        List<Post> userPosts = postsRepository.findByUserId(999L);

        // Then
        assertTrue(userPosts.isEmpty());
    }

    @Test
    void findByUserId_ShouldReturnPostsOrderedByCreatedAtDesc() {
        // Given
        Post oldPost = new Post();
        oldPost.setContent("Old post");
        oldPost.setUserId(100L);
        oldPost.setCreatedAt(LocalDateTime.now().minusDays(10));

        Post newPost = new Post();
        newPost.setContent("New post");
        newPost.setUserId(100L);
        newPost.setCreatedAt(LocalDateTime.now());

        Post middlePost = new Post();
        middlePost.setContent("Middle post");
        middlePost.setUserId(100L);
        middlePost.setCreatedAt(LocalDateTime.now().minusDays(5));

        entityManager.persistAndFlush(oldPost);
        entityManager.persistAndFlush(middlePost);
        entityManager.persistAndFlush(newPost);

        // When
        List<Post> userPosts = postsRepository.findByUserId(100L);

        // Then
        assertEquals(3, userPosts.size());
        assertEquals("New post", userPosts.get(0).getContent());
        assertEquals("Middle post", userPosts.get(1).getContent());
        assertEquals("Old post", userPosts.get(2).getContent());
    }

    @Test
    void existsById_ShouldReturnTrue_WhenPostExists() {
        // Given
        Post savedPost = entityManager.persistAndFlush(testPost1);

        // When
        boolean exists = postsRepository.existsById(savedPost.getId());

        // Then
        assertTrue(exists);
    }

    @Test
    void existsById_ShouldReturnFalse_WhenPostDoesNotExist() {
        // When
        boolean exists = postsRepository.existsById(999L);

        // Then
        assertFalse(exists);
    }

    @Test
    void delete_ShouldRemovePost_WhenPostExists() {
        // Given
        Post savedPost = entityManager.persistAndFlush(testPost1);
        Long postId = savedPost.getId();

        // When
        postsRepository.delete(savedPost);
        entityManager.flush();

        // Then
        Optional<Post> deletedPost = postsRepository.findById(postId);
        assertFalse(deletedPost.isPresent());
    }

    @Test
    void findAll_ShouldReturnAllPosts() {
        // Given
        entityManager.persistAndFlush(testPost1);
        entityManager.persistAndFlush(testPost2);
        entityManager.persistAndFlush(testPost3);

        // When
        List<Post> allPosts = postsRepository.findAll();

        // Then
        assertEquals(3, allPosts.size());
    }

    @Test
    void save_ShouldHandlePostWithoutImage() {
        // Given
        testPost1.setImageUrl(null);

        // When
        Post savedPost = postsRepository.save(testPost1);

        // Then
        assertNotNull(savedPost.getId());
        assertEquals(testPost1.getContent(), savedPost.getContent());
        assertNull(savedPost.getImageUrl());
    }

    @Test
    void save_ShouldHandlePostWithLongContent() {
        // Given
        String longContent = "A".repeat(1000); // 1000 character content
        testPost1.setContent(longContent);

        // When
        Post savedPost = postsRepository.save(testPost1);

        // Then
        assertNotNull(savedPost.getId());
        assertEquals(longContent, savedPost.getContent());
    }

    @Test
    void save_ShouldHandlePostWithSpecialCharacters() {
        // Given
        String specialContent = "Post with special chars: @#$%^&*()_+{}|:<>?[]\\;'\",./ and emojis 😀🎉";
        testPost1.setContent(specialContent);

        // When
        Post savedPost = postsRepository.save(testPost1);

        // Then
        assertNotNull(savedPost.getId());
        assertEquals(specialContent, savedPost.getContent());
    }

    @Test
    void findByUserId_ShouldHandleUserWithManyPosts() {
        // Given
        Long userId = 100L;
        for (int i = 0; i < 50; i++) {
            Post post = new Post();
            post.setContent("Post number " + i);
            post.setUserId(userId);
            post.setCreatedAt(LocalDateTime.now().minusDays(i));
            entityManager.persist(post);
        }
        entityManager.flush();

        // When
        List<Post> userPosts = postsRepository.findByUserId(userId);

        // Then
        assertEquals(50, userPosts.size());
        // Verify ordering (newest first)
        assertEquals("Post number 0", userPosts.get(0).getContent());
        assertEquals("Post number 49", userPosts.get(49).getContent());
    }

    @Test
    void save_ShouldUpdateExistingPost() {
        // Given
        Post savedPost = entityManager.persistAndFlush(testPost1);
        String updatedContent = "Updated content";

        // When
        savedPost.setContent(updatedContent);
        Post updatedPost = postsRepository.save(savedPost);

        // Then
        assertEquals(savedPost.getId(), updatedPost.getId());
        assertEquals(updatedContent, updatedPost.getContent());
    }
}
