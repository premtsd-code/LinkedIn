package com.premtsd.linkedin.notificationservice.consumer;

import com.premtsd.linkedin.notificationservice.service.EventDeduplicator;
import com.premtsd.linkedin.notificationservice.service.SendEmail;
import com.premtsd.linkedin.userservice.event.UserCreatedEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class SendEmailConsumer {

    private final SendEmail sendEmail;
    private final EventDeduplicator eventDeduplicator;

    // NOTE: no try/catch swallowing here — a failure must propagate so the
    // DefaultErrorHandler can retry and eventually dead-letter the record
    // (userCreatedTopic.DLT) instead of silently losing it.
    @KafkaListener(topics = "userCreatedTopic")
    @Transactional  // dedup marker + effects commit atomically
    public void handleUserCreatedEmail(@Payload UserCreatedEmailEvent userCreatedEmailEvent,
                                       @Header(name = "eventId", required = false) String eventId) {
        log.info("Received user created email event {} for: {}", eventId, userCreatedEmailEvent.getTo());
        if (eventDeduplicator.alreadyProcessed(eventId)) {
            return; // at-least-once redelivery — already handled
        }
        sendEmail.sendEmail(userCreatedEmailEvent.getTo(), userCreatedEmailEvent.getSubject(), userCreatedEmailEvent.getBody());
        eventDeduplicator.markProcessed(eventId);
        log.info("User created email processed successfully for: {}", userCreatedEmailEvent.getTo());
    }

}
