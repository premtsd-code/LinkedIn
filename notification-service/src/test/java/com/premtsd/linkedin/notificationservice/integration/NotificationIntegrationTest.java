package com.premtsd.linkedin.notificationservice.integration;

import com.premtsd.linkedin.notificationservice.auth.UserContextHolder;
import com.premtsd.linkedin.notificationservice.clients.ConnectionsClient;
import com.premtsd.linkedin.notificationservice.entity.Notification;
import com.premtsd.linkedin.notificationservice.repository.NotificationRepository;
import com.premtsd.linkedin.notificationservice.service.SendNotification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mockStatic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.config.import=optional:configserver:",
        "spring.kafka.listener.auto-startup=false",
        "server.servlet.context-path="
})
class NotificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SendNotification sendNotification;

    @Autowired
    private NotificationRepository notificationRepository;

    @MockBean
    private ConnectionsClient connectionsClient;

    @MockBean
    private JavaMailSender javaMailSender;

    @MockBean
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

    @MockBean
    private ConsumerFactory<?, ?> consumerFactory;

    private MockedStatic<UserContextHolder> userContextHolderMock;

    private static final Long TEST_USER_ID = 100L;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userContextHolderMock = mockStatic(UserContextHolder.class);
        userContextHolderMock.when(UserContextHolder::getCurrentUserId).thenReturn(TEST_USER_ID);
    }

    @AfterEach
    void tearDown() {
        userContextHolderMock.close();
        notificationRepository.deleteAll();
    }

    @Test
    void fullNotificationLifecycle() throws Exception {
        // Create notifications via SendNotification
        sendNotification.send(TEST_USER_ID, "First notification");
        sendNotification.send(TEST_USER_ID, "Second notification");

        // GET all notifications
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].message", containsInAnyOrder("First notification", "Second notification")))
                .andExpect(jsonPath("$[0].isRead", is(false)))
                .andExpect(jsonPath("$[1].isRead", is(false)));

        // Get the ID of the first notification from the database
        Notification notification = notificationRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID).get(0);
        Long notificationId = notification.getId();

        // Mark as read
        mockMvc.perform(put("/notifications/{notificationId}/read", notificationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(notificationId.intValue())))
                .andExpect(jsonPath("$.isRead", is(true)));

        // Verify the notification is now read in the database
        Notification updated = notificationRepository.findById(notificationId).orElseThrow();
        assert updated.getIsRead();
    }

    @Test
    void unreadNotifications() throws Exception {
        // Create notifications
        sendNotification.send(TEST_USER_ID, "Unread notification 1");
        sendNotification.send(TEST_USER_ID, "Unread notification 2");
        sendNotification.send(TEST_USER_ID, "Read notification");

        // Mark one as read directly in the database
        Notification readNotification = notificationRepository.findByUserIdOrderByCreatedAtDesc(TEST_USER_ID)
                .stream()
                .filter(n -> n.getMessage().equals("Read notification"))
                .findFirst()
                .orElseThrow();
        readNotification.setIsRead(true);
        notificationRepository.save(readNotification);

        // GET unread notifications
        mockMvc.perform(get("/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].message", containsInAnyOrder("Unread notification 1", "Unread notification 2")))
                .andExpect(jsonPath("$[0].isRead", is(false)))
                .andExpect(jsonPath("$[1].isRead", is(false)));
    }

    @Test
    void markAsRead_ShouldReturn404_WhenNotificationDoesNotExist() throws Exception {
        mockMvc.perform(put("/notifications/{notificationId}/read", 99999L))
                .andExpect(status().isNotFound());
    }
}
