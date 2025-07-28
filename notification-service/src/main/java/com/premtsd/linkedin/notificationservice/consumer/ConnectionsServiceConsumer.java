package com.premtsd.linkedin.notificationservice.consumer;

import com.premtsd.linkedin.connectionservice.event.AcceptConnectionRequestEvent;
import com.premtsd.linkedin.notificationservice.service.SendNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ConnectionsServiceConsumer {

    private final SendNotification sendNotification;

    @KafkaListener(topics = "send-connection-request-topic")
    public void handleSendConnectionRequest(com.premtsd.linkedin.connectionservice.event.SendConnectionRequestEvent sendConnectionRequestEvent) {
        log.info("Received send connection request event: sender={}, receiver={}",
                sendConnectionRequestEvent.getSenderId(), sendConnectionRequestEvent.getReceiverId());
        try {
            String message =
                    "You have receiver a connection request from user with id: " + sendConnectionRequestEvent.getSenderId();
            sendNotification.send(sendConnectionRequestEvent.getReceiverId(), message);
            log.info("Connection request notification sent successfully to user: {}", sendConnectionRequestEvent.getReceiverId());
        } catch (Exception e) {
            log.error("Failed to send connection request notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "accept-connection-request-topic")
    public void handleAcceptConnectionRequest(AcceptConnectionRequestEvent acceptConnectionRequestEvent) {
        log.info("Received accept connection request event: sender={}, receiver={}",
                acceptConnectionRequestEvent.getSenderId(), acceptConnectionRequestEvent.getReceiverId());
        try {
            String message =
                    "Your connection request has been accepted by the user with id: " + acceptConnectionRequestEvent.getReceiverId();
            sendNotification.send(acceptConnectionRequestEvent.getSenderId(), message);
            log.info("Connection acceptance notification sent successfully to user: {}", acceptConnectionRequestEvent.getSenderId());
        } catch (Exception e) {
            log.error("Failed to send connection acceptance notification: {}", e.getMessage());
        }
    }

}
