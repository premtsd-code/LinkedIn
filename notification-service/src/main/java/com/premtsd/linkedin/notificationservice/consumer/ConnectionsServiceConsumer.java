package com.premtsd.linkedin.notificationservice.consumer;

import com.premtsd.linkedin.connectionservice.event.AcceptConnectionRequestEvent;
import com.premtsd.linkedin.notificationservice.service.EventDeduplicator;
import com.premtsd.linkedin.notificationservice.service.SendNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Failures propagate to the DefaultErrorHandler (retry -> <topic>.DLT); the
// eventId header enables dedup once connections-service adopts the outbox.
@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionsServiceConsumer {

    private final SendNotification sendNotification;
    private final EventDeduplicator eventDeduplicator;

    @KafkaListener(topics = "send-connection-request-topic")
    @Transactional
    public void handleSendConnectionRequest(@Payload com.premtsd.linkedin.connectionservice.event.SendConnectionRequestEvent sendConnectionRequestEvent,
                                            @Header(name = "eventId", required = false) String eventId) {
        log.info("Received send connection request event: sender={}, receiver={}",
                sendConnectionRequestEvent.getSenderId(), sendConnectionRequestEvent.getReceiverId());
        if (eventDeduplicator.alreadyProcessed(eventId)) {
            return;
        }
        String message =
                "You have receiver a connection request from user with id: " + sendConnectionRequestEvent.getSenderId();
        sendNotification.send(sendConnectionRequestEvent.getReceiverId(), message);
        eventDeduplicator.markProcessed(eventId);
        log.info("Connection request notification sent successfully to user: {}", sendConnectionRequestEvent.getReceiverId());
    }

    @KafkaListener(topics = "accept-connection-request-topic")
    @Transactional
    public void handleAcceptConnectionRequest(@Payload AcceptConnectionRequestEvent acceptConnectionRequestEvent,
                                              @Header(name = "eventId", required = false) String eventId) {
        log.info("Received accept connection request event: sender={}, receiver={}",
                acceptConnectionRequestEvent.getSenderId(), acceptConnectionRequestEvent.getReceiverId());
        if (eventDeduplicator.alreadyProcessed(eventId)) {
            return;
        }
        String message =
                "Your connection request has been accepted by the user with id: " + acceptConnectionRequestEvent.getReceiverId();
        sendNotification.send(acceptConnectionRequestEvent.getSenderId(), message);
        eventDeduplicator.markProcessed(eventId);
        log.info("Connection acceptance notification sent successfully to user: {}", acceptConnectionRequestEvent.getSenderId());
    }

}
