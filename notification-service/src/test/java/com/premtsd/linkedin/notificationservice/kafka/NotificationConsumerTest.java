package com.premtsd.linkedin.notificationservice.kafka;

import com.premtsd.linkedin.connectionservice.event.AcceptConnectionRequestEvent;
import com.premtsd.linkedin.connectionservice.event.SendConnectionRequestEvent;
import com.premtsd.linkedin.notificationservice.clients.ConnectionsClient;
import com.premtsd.linkedin.notificationservice.consumer.ConnectionsServiceConsumer;
import com.premtsd.linkedin.notificationservice.consumer.PostsServiceConsumer;
import com.premtsd.linkedin.notificationservice.consumer.SendEmailConsumer;
import com.premtsd.linkedin.notificationservice.dto.PersonDto;
import com.premtsd.linkedin.notificationservice.service.SendEmail;
import com.premtsd.linkedin.notificationservice.service.SendNotification;
import com.premtsd.linkedin.postservice.event.PostCreatedEvent;
import com.premtsd.linkedin.postservice.event.PostLikedEvent;
import com.premtsd.linkedin.userservice.event.UserCreatedEmailEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock
    private SendNotification sendNotification;

    @Mock
    private ConnectionsClient connectionsClient;

    @Mock
    private SendEmail sendEmail;

    @InjectMocks
    private ConnectionsServiceConsumer connectionsServiceConsumer;

    @InjectMocks
    private PostsServiceConsumer postsServiceConsumer;

    @InjectMocks
    private SendEmailConsumer sendEmailConsumer;

    // --- ConnectionsServiceConsumer tests ---

    @Test
    void handleSendConnectionRequest_ShouldCreateNotification() {
        SendConnectionRequestEvent event = new SendConnectionRequestEvent();
        event.setSenderId(1L);
        event.setReceiverId(2L);

        connectionsServiceConsumer.handleSendConnectionRequest(event);

        verify(sendNotification).send(
                eq(2L),
                eq("You have receiver a connection request from user with id: 1")
        );
    }

    @Test
    void handleAcceptConnectionRequest_ShouldCreateNotification() {
        AcceptConnectionRequestEvent event = new AcceptConnectionRequestEvent();
        event.setSenderId(1L);
        event.setReceiverId(2L);

        connectionsServiceConsumer.handleAcceptConnectionRequest(event);

        verify(sendNotification).send(
                eq(1L),
                eq("Your connection request has been accepted by the user with id: 2")
        );
    }

    // --- PostsServiceConsumer tests ---

    @Test
    void handlePostCreated_ShouldNotifyAllConnections() {
        PostCreatedEvent event = new PostCreatedEvent();
        event.setCreatorId(10L);
        event.setContent("Hello world");
        event.setPostId(100L);

        PersonDto connection1 = new PersonDto();
        connection1.setId(1L);
        connection1.setUserId(20L);
        connection1.setName("Alice");

        PersonDto connection2 = new PersonDto();
        connection2.setId(2L);
        connection2.setUserId(30L);
        connection2.setName("Bob");

        when(connectionsClient.getFirstConnections(10L)).thenReturn(List.of(connection1, connection2));

        postsServiceConsumer.handlePostCreated(event);

        verify(connectionsClient).getFirstConnections(10L);
        verify(sendNotification).send(eq(20L), eq("Your connection 10 has created a post, Check it out"));
        verify(sendNotification).send(eq(30L), eq("Your connection 10 has created a post, Check it out"));
        verifyNoMoreInteractions(sendNotification);
    }

    @Test
    void handlePostLiked_ShouldNotifyPostCreator() {
        PostLikedEvent event = new PostLikedEvent();
        event.setPostId(100L);
        event.setCreatorId(10L);
        event.setLikedByUserId(20L);

        postsServiceConsumer.handlePostLiked(event);

        verify(sendNotification).send(
                eq(10L),
                eq(String.format("Your post, %d has been liked by %d", 100L, 20L))
        );
    }

    // --- SendEmailConsumer tests ---

    @Test
    void handleUserCreatedEmail_ShouldSendEmail() {
        UserCreatedEmailEvent event = new UserCreatedEmailEvent();
        event.setTo("test@example.com");
        event.setSubject("Welcome");
        event.setBody("Welcome to LinkedIn!");

        sendEmailConsumer.handleUserCreatedEmail(event);

        verify(sendEmail).sendEmail("test@example.com", "Welcome", "Welcome to LinkedIn!");
    }
}
