package com.premtsd.linkedin.notificationservice.consumer;

import com.premtsd.linkedin.notificationservice.clients.ConnectionsClient;
import com.premtsd.linkedin.notificationservice.dto.PersonDto;
import com.premtsd.linkedin.notificationservice.service.EventDeduplicator;
import com.premtsd.linkedin.notificationservice.service.SendNotification;
import com.premtsd.linkedin.postservice.event.PostCreatedEvent;
import com.premtsd.linkedin.postservice.event.PostLikedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Failures propagate to the DefaultErrorHandler (retry -> <topic>.DLT); the
// eventId header enables dedup once post-service adopts the outbox pattern.
@Service
@Slf4j
@RequiredArgsConstructor
public class PostsServiceConsumer {

    private final ConnectionsClient connectionsClient;
    private final SendNotification sendNotification;
    private final EventDeduplicator eventDeduplicator;

    @KafkaListener(topics = "post-created-topic")
    @Transactional
    public void handlePostCreated(@Payload PostCreatedEvent postCreatedEvent,
                                  @Header(name = "eventId", required = false) String eventId) {
        log.info("Received post created event: postId={}, creatorId={}",
                postCreatedEvent.getPostId(), postCreatedEvent.getCreatorId());
        if (eventDeduplicator.alreadyProcessed(eventId)) {
            return;
        }
        List<PersonDto> connections = connectionsClient.getFirstConnections(postCreatedEvent.getCreatorId());
        log.debug("Found {} connections for user: {}", connections.size(), postCreatedEvent.getCreatorId());

        for(PersonDto connection: connections) {
            String message = "Your connection " + postCreatedEvent.getCreatorId() + " has created a post, Check it out";
            sendNotification.send(connection.getUserId(), message);
        }
        eventDeduplicator.markProcessed(eventId);
        log.info("Post creation notifications sent to {} connections", connections.size());
    }

    @KafkaListener(topics = "post-liked-topic")
    @Transactional
    public void handlePostLiked(@Payload PostLikedEvent postLikedEvent,
                                @Header(name = "eventId", required = false) String eventId) {
        log.info("Received post liked event: postId={}, likedBy={}, creator={}",
                postLikedEvent.getPostId(), postLikedEvent.getLikedByUserId(), postLikedEvent.getCreatorId());
        if (eventDeduplicator.alreadyProcessed(eventId)) {
            return;
        }
        String message = String.format("Your post, %d has been liked by %d", postLikedEvent.getPostId(),
                postLikedEvent.getLikedByUserId());

        sendNotification.send(postLikedEvent.getCreatorId(), message);
        eventDeduplicator.markProcessed(eventId);
        log.info("Post liked notification sent to creator: {}", postLikedEvent.getCreatorId());
    }

}
