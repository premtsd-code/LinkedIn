package com.premtsd.linkedin.notificationservice.consumer;

import com.premtsd.linkedin.notificationservice.service.SendEmail;
import com.premtsd.linkedin.userservice.event.UserCreatedEmailEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class SendEmailConsumer {

    private final SendEmail sendEmail;
    @KafkaListener(topics = "userCreatedTopic")
    public void handleUserCreatedEmail(UserCreatedEmailEvent userCreatedEmailEvent) {
        log.info("Received user created email event for: {}", userCreatedEmailEvent.getTo());
        try {
            sendEmail.sendEmail(userCreatedEmailEvent.getTo(), userCreatedEmailEvent.getSubject(), userCreatedEmailEvent.getBody());
            log.info("User created email processed successfully for: {}", userCreatedEmailEvent.getTo());
        } catch (Exception e) {
            log.error("Failed to process user created email for: {}. Error: {}", userCreatedEmailEvent.getTo(), e.getMessage());
        }
    }

}
