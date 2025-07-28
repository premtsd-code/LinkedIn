package com.premtsd.linkedin.notificationservice.consumer;

import com.premtsd.linkedin.notificationservice.clients.ConnectionsClient;
import com.premtsd.linkedin.notificationservice.dto.PersonDto;
import com.premtsd.linkedin.notificationservice.service.SendNotification;
import com.premtsd.linkedin.postservice.event.PostCreatedEvent;
import com.premtsd.linkedin.postservice.event.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostsServiceConsumer {

    private final ConnectionsClient connectionsClient;
    private final SendNotification sendNotification;

    @KafkaListener(topics = "post-created-topic")
    public void handlePostCreated(PostCreatedEvent postCreatedEvent) {
        log.info("Received post created event: postId={}, creatorId={}",
                postCreatedEvent.getPostId(), postCreatedEvent.getCreatorId());
        try {
            List<PersonDto> connections = connectionsClient.getFirstConnections(postCreatedEvent.getCreatorId());
            log.debug("Found {} connections for user: {}", connections.size(), postCreatedEvent.getCreatorId());

            for(PersonDto connection: connections) {
                String message = "Your connection " + postCreatedEvent.getCreatorId() + " has created a post, Check it out";
                sendNotification.send(connection.getUserId(), message);
            }
            log.info("Post creation notifications sent to {} connections", connections.size());
        } catch (Exception e) {
            log.error("Failed to send post creation notifications: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "post-liked-topic")
    public void handlePostLiked(PostLikedEvent postLikedEvent) {
        log.info("Received post liked event: postId={}, likedBy={}, creator={}",
                postLikedEvent.getPostId(), postLikedEvent.getLikedByUserId(), postLikedEvent.getCreatorId());
        try {
            String message = String.format("Your post, %d has been liked by %d", postLikedEvent.getPostId(),
                    postLikedEvent.getLikedByUserId());

            sendNotification.send(postLikedEvent.getCreatorId(), message);
            log.info("Post liked notification sent to creator: {}", postLikedEvent.getCreatorId());
        } catch (Exception e) {
            log.error("Failed to send post liked notification: {}", e.getMessage());
        }
    }

}
