package com.premtsd.linkedin.notificationservice.service;

import com.premtsd.linkedin.notificationservice.dto.NotificationDto;
import com.premtsd.linkedin.notificationservice.entity.Notification;
import com.premtsd.linkedin.notificationservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    public List<NotificationDto> getAllNotifications(Long userId) {
        log.info("Fetching all notifications for user: {}", userId);
        try {
            List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
            log.info("Found {} notifications for user: {}", notifications.size(), userId);
            
            return notifications.stream()
                    .map(notification -> modelMapper.map(notification, NotificationDto.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to fetch notifications for user: {}. Error: {}", userId, e.getMessage());
            throw e;
        }
    }

    public List<NotificationDto> getUnreadNotifications(Long userId) {
        log.info("Fetching unread notifications for user: {}", userId);
        try {
            List<Notification> notifications = notificationRepository.findUnreadByUserIdOrderByCreatedAtDesc(userId);
            log.info("Found {} unread notifications for user: {}", notifications.size(), userId);
            
            return notifications.stream()
                    .map(notification -> modelMapper.map(notification, NotificationDto.class))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to fetch unread notifications for user: {}. Error: {}", userId, e.getMessage());
            throw e;
        }
    }

    public NotificationDto markAsRead(Long notificationId, Long userId) {
        log.info("Marking notification {} as read for user: {}", notificationId, userId);
        try {
            Notification notification = notificationRepository.findById(notificationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + notificationId));

            // Verify that the notification belongs to the user
            if (!notification.getUserId().equals(userId)) {
                log.warn("User {} attempted to mark notification {} as read, but it belongs to user {}", 
                        userId, notificationId, notification.getUserId());
                throw new ResourceNotFoundException("Notification not found with id: " + notificationId);
            }

            notification.setIsRead(true);
            Notification savedNotification = notificationRepository.save(notification);
            log.info("Notification {} marked as read successfully for user: {}", notificationId, userId);
            
            return modelMapper.map(savedNotification, NotificationDto.class);
        } catch (Exception e) {
            log.error("Failed to mark notification {} as read for user: {}. Error: {}", notificationId, userId, e.getMessage());
            throw e;
        }
    }
}
