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
        System.out.println("Message sent successfully");
        log.info("handleUserCreatedEmail: {}", userCreatedEmailEvent);
        sendEmail.sendEmail(userCreatedEmailEvent.getTo(), userCreatedEmailEvent.getSubject(), userCreatedEmailEvent.getBody());
    }

}
