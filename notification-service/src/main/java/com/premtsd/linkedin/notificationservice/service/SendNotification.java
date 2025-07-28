package com.premtsd.linkedin.notificationservice.service;

import com.premtsd.linkedin.notificationservice.entity.Notification;
import com.premtsd.linkedin.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendNotification {

    private final NotificationRepository notificationRepository;

    public void send(Long userId, String message) {
        log.info("Sending notification to user: {} with message: {}", userId, message);
        try {
            Notification notification = new Notification();
            notification.setMessage(message);
            notification.setUserId(userId);

            notificationRepository.save(notification);
            log.info("Notification saved successfully for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to save notification for user: {}. Error: {}", userId, e.getMessage());
            throw e;
        }
    }
}
