package com.premtsd.linkedin.notificationservice.repository;

import com.premtsd.linkedin.notificationservice.entity.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.config.import=optional:configserver:",
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "spring.kafka.bootstrap-servers=localhost:9092"
})
class NotificationRepositoryTest {

    @Autowired
    private NotificationRepository notificationRepository;

    private static final Long USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
    }

    @Test
    void shouldReturnNotificationsOrderedByCreatedAtDescWhenFindByUserId() {
        // Given
        Notification notification1 = new Notification();
        notification1.setUserId(USER_ID);
        notification1.setMessage("First notification");
        notification1.setIsRead(false);
        notificationRepository.save(notification1);

        Notification notification2 = new Notification();
        notification2.setUserId(USER_ID);
        notification2.setMessage("Second notification");
        notification2.setIsRead(true);
        notificationRepository.save(notification2);

        Notification otherUserNotification = new Notification();
        otherUserNotification.setUserId(OTHER_USER_ID);
        otherUserNotification.setMessage("Other user notification");
        otherUserNotification.setIsRead(false);
        notificationRepository.save(otherUserNotification);

        // When
        List<Notification> result = notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(n -> n.getUserId().equals(USER_ID));
    }

    @Test
    void shouldReturnOnlyUnreadNotificationsWhenFindUnreadByUserId() {
        // Given
        Notification unreadNotification1 = new Notification();
        unreadNotification1.setUserId(USER_ID);
        unreadNotification1.setMessage("Unread notification 1");
        unreadNotification1.setIsRead(false);
        notificationRepository.save(unreadNotification1);

        Notification readNotification = new Notification();
        readNotification.setUserId(USER_ID);
        readNotification.setMessage("Read notification");
        readNotification.setIsRead(true);
        notificationRepository.save(readNotification);

        Notification unreadNotification2 = new Notification();
        unreadNotification2.setUserId(USER_ID);
        unreadNotification2.setMessage("Unread notification 2");
        unreadNotification2.setIsRead(false);
        notificationRepository.save(unreadNotification2);

        // When
        List<Notification> result = notificationRepository.findUnreadByUserIdOrderByCreatedAtDesc(USER_ID);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(n -> !n.getIsRead());
        assertThat(result).allMatch(n -> n.getUserId().equals(USER_ID));
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoNotifications() {
        // Given
        Notification otherUserNotification = new Notification();
        otherUserNotification.setUserId(OTHER_USER_ID);
        otherUserNotification.setMessage("Other user notification");
        otherUserNotification.setIsRead(false);
        notificationRepository.save(otherUserNotification);

        // When
        List<Notification> allResult = notificationRepository.findByUserIdOrderByCreatedAtDesc(USER_ID);
        List<Notification> unreadResult = notificationRepository.findUnreadByUserIdOrderByCreatedAtDesc(USER_ID);

        // Then
        assertThat(allResult).isEmpty();
        assertThat(unreadResult).isEmpty();
    }

    @Test
    void shouldReturnUnreadNotificationsInDescendingOrderByCreatedAt() {
        // Given
        Notification notification1 = new Notification();
        notification1.setUserId(USER_ID);
        notification1.setMessage("Older unread");
        notification1.setIsRead(false);
        notificationRepository.save(notification1);

        Notification notification2 = new Notification();
        notification2.setUserId(USER_ID);
        notification2.setMessage("Newer unread");
        notification2.setIsRead(false);
        notificationRepository.save(notification2);

        // When
        List<Notification> result = notificationRepository.findUnreadByUserIdOrderByCreatedAtDesc(USER_ID);

        // Then
        assertThat(result).hasSize(2);
    }
}
