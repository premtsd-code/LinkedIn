package com.premtsd.linkedin.notificationservice.service;

import com.premtsd.linkedin.notificationservice.dto.NotificationDto;
import com.premtsd.linkedin.notificationservice.entity.Notification;
import com.premtsd.linkedin.notificationservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ModelMapper modelMapper;

    @InjectMocks
    private NotificationService notificationService;

    private Notification notification;
    private NotificationDto notificationDto;
    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 100L;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setId(NOTIFICATION_ID);
        notification.setUserId(USER_ID);
        notification.setMessage("You have a new connection request");
        notification.setIsRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationDto = new NotificationDto();
        notificationDto.setId(NOTIFICATION_ID);
        notificationDto.setUserId(USER_ID);
        notificationDto.setMessage("You have a new connection request");
        notificationDto.setIsRead(false);
        notificationDto.setCreatedAt(notification.getCreatedAt());
    }

    @Test
    void shouldReturnAllNotificationsWhenUserHasNotifications() {
        // Given
        Notification notification2 = new Notification();
        notification2.setId(101L);
        notification2.setUserId(USER_ID);
        notification2.setMessage("Someone liked your post");
        notification2.setIsRead(true);

        NotificationDto dto2 = new NotificationDto();
        dto2.setId(101L);
        dto2.setUserId(USER_ID);
        dto2.setMessage("Someone liked your post");
        dto2.setIsRead(true);

        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(notification, notification2));
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);
        when(modelMapper.map(notification2, NotificationDto.class)).thenReturn(dto2);

        // When
        List<NotificationDto> result = notificationService.getAllNotifications(USER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(NOTIFICATION_ID);
        assertThat(result.get(1).getId()).isEqualTo(101L);
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(USER_ID);
        verify(modelMapper).map(notification, NotificationDto.class);
        verify(modelMapper).map(notification2, NotificationDto.class);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoNotifications() {
        // Given
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(Collections.emptyList());

        // When
        List<NotificationDto> result = notificationService.getAllNotifications(USER_ID);

        // Then
        assertThat(result).isEmpty();
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(USER_ID);
        verify(modelMapper, never()).map(any(Notification.class), eq(NotificationDto.class));
    }

    @Test
    void shouldReturnUnreadNotificationsWhenUserHasUnreadNotifications() {
        // Given
        when(notificationRepository.findUnreadByUserIdOrderByCreatedAtDesc(USER_ID))
                .thenReturn(List.of(notification));
        when(modelMapper.map(notification, NotificationDto.class)).thenReturn(notificationDto);

        // When
        List<NotificationDto> result = notificationService.getUnreadNotifications(USER_ID);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getIsRead()).isFalse();
        assertThat(result.get(0).getMessage()).isEqualTo("You have a new connection request");
        verify(notificationRepository).findUnreadByUserIdOrderByCreatedAtDesc(USER_ID);
        verify(modelMapper).map(notification, NotificationDto.class);
    }

    @Test
    void shouldMarkNotificationAsReadWhenNotificationExistsAndBelongsToUser() {
        // Given
        NotificationDto readDto = new NotificationDto();
        readDto.setId(NOTIFICATION_ID);
        readDto.setUserId(USER_ID);
        readDto.setMessage("You have a new connection request");
        readDto.setIsRead(true);

        Notification savedNotification = new Notification();
        savedNotification.setId(NOTIFICATION_ID);
        savedNotification.setUserId(USER_ID);
        savedNotification.setMessage("You have a new connection request");
        savedNotification.setIsRead(true);

        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(savedNotification);
        when(modelMapper.map(savedNotification, NotificationDto.class)).thenReturn(readDto);

        // When
        NotificationDto result = notificationService.markAsRead(NOTIFICATION_ID, USER_ID);

        // Then
        assertThat(result.getIsRead()).isTrue();
        assertThat(result.getId()).isEqualTo(NOTIFICATION_ID);
        verify(notificationRepository).findById(NOTIFICATION_ID);
        verify(notificationRepository).save(notification);
        verify(modelMapper).map(savedNotification, NotificationDto.class);
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenNotificationDoesNotExist() {
        // Given
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.empty());

        // When / Then
        assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found with id: " + NOTIFICATION_ID);

        verify(notificationRepository).findById(NOTIFICATION_ID);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldThrowResourceNotFoundExceptionWhenNotificationBelongsToDifferentUser() {
        // Given
        Long differentUserId = 999L;
        when(notificationRepository.findById(NOTIFICATION_ID)).thenReturn(Optional.of(notification));

        // When / Then
        assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, differentUserId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Notification not found with id: " + NOTIFICATION_ID);

        verify(notificationRepository).findById(NOTIFICATION_ID);
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
