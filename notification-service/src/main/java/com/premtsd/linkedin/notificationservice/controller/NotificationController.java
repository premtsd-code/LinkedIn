package com.premtsd.linkedin.notificationservice.controller;

import com.premtsd.linkedin.notificationservice.auth.UserContextHolder;
import com.premtsd.linkedin.notificationservice.dto.NotificationDto;
import com.premtsd.linkedin.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * List all notifications for the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getAllNotifications() {
        Long userId = UserContextHolder.getCurrentUserId();
        log.info("GET /notifications - Fetching all notifications for user: {}", userId);
        List<NotificationDto> notifications = notificationService.getAllNotifications(userId);
        log.info("Returning {} notifications for user: {}", notifications.size(), userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * List only unread notifications for the authenticated user
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDto>> getUnreadNotifications() {
        Long userId = UserContextHolder.getCurrentUserId();
        log.info("GET /notifications/unread - Fetching unread notifications for user: {}", userId);
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(userId);
        log.info("Returning {} unread notifications for user: {}", notifications.size(), userId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Mark a specific notification as read
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationDto> markNotificationAsRead(
            @PathVariable Long notificationId) {
        Long userId = UserContextHolder.getCurrentUserId();
        log.info("PUT /notifications/{}/read - Marking notification as read for user: {}", notificationId, userId);
        NotificationDto notification = notificationService.markAsRead(notificationId, userId);
        log.info("Notification {} marked as read successfully for user: {}", notificationId, userId);
        return ResponseEntity.ok(notification);
    }
}
