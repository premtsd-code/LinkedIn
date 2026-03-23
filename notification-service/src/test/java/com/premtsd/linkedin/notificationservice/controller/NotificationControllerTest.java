package com.premtsd.linkedin.notificationservice.controller;

import com.premtsd.linkedin.notificationservice.auth.UserContextHolder;
import com.premtsd.linkedin.notificationservice.dto.NotificationDto;
import com.premtsd.linkedin.notificationservice.exception.ResourceNotFoundException;
import com.premtsd.linkedin.notificationservice.service.NotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
@org.springframework.test.context.TestPropertySource(properties = {
    "spring.config.import=optional:configserver:",
    "spring.cloud.config.enabled=false",
    "eureka.client.enabled=false",
    "server.servlet.context-path="
})
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    private MockedStatic<UserContextHolder> userContextHolderMock;

    private static final Long USER_ID = 1L;
    private static final Long NOTIFICATION_ID = 100L;

    private NotificationDto notificationDto;

    @BeforeEach
    void setUp() {
        userContextHolderMock = Mockito.mockStatic(UserContextHolder.class);
        userContextHolderMock.when(UserContextHolder::getCurrentUserId).thenReturn(USER_ID);

        notificationDto = new NotificationDto();
        notificationDto.setId(NOTIFICATION_ID);
        notificationDto.setUserId(USER_ID);
        notificationDto.setMessage("You have a new connection request");
        notificationDto.setIsRead(false);
        notificationDto.setCreatedAt(LocalDateTime.of(2026, 3, 17, 10, 0, 0));
    }

    @AfterEach
    void tearDown() {
        userContextHolderMock.close();
    }

    @Test
    void shouldReturnAllNotificationsWhenGetNotificationsIsCalled() throws Exception {
        // Given
        NotificationDto dto2 = new NotificationDto();
        dto2.setId(101L);
        dto2.setUserId(USER_ID);
        dto2.setMessage("Someone liked your post");
        dto2.setIsRead(true);
        dto2.setCreatedAt(LocalDateTime.of(2026, 3, 16, 10, 0, 0));

        when(notificationService.getAllNotifications(USER_ID))
                .thenReturn(List.of(notificationDto, dto2));

        // When / Then
        mockMvc.perform(get("/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(NOTIFICATION_ID))
                .andExpect(jsonPath("$[0].message").value("You have a new connection request"))
                .andExpect(jsonPath("$[1].id").value(101));

        verify(notificationService).getAllNotifications(USER_ID);
    }

    @Test
    void shouldReturnUnreadNotificationsWhenGetUnreadIsCalled() throws Exception {
        // Given
        when(notificationService.getUnreadNotifications(USER_ID))
                .thenReturn(List.of(notificationDto));

        // When / Then
        mockMvc.perform(get("/notifications/unread")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(NOTIFICATION_ID))
                .andExpect(jsonPath("$[0].isRead").value(false));

        verify(notificationService).getUnreadNotifications(USER_ID);
    }

    @Test
    void shouldReturnOkWhenMarkingNotificationAsRead() throws Exception {
        // Given
        NotificationDto readDto = new NotificationDto();
        readDto.setId(NOTIFICATION_ID);
        readDto.setUserId(USER_ID);
        readDto.setMessage("You have a new connection request");
        readDto.setIsRead(true);
        readDto.setCreatedAt(LocalDateTime.of(2026, 3, 17, 10, 0, 0));

        when(notificationService.markAsRead(NOTIFICATION_ID, USER_ID)).thenReturn(readDto);

        // When / Then
        mockMvc.perform(put("/notifications/{notificationId}/read", NOTIFICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(NOTIFICATION_ID))
                .andExpect(jsonPath("$.isRead").value(true));

        verify(notificationService).markAsRead(NOTIFICATION_ID, USER_ID);
    }

    @Test
    void shouldReturn404WhenMarkingNonExistentNotificationAsRead() throws Exception {
        // Given
        when(notificationService.markAsRead(NOTIFICATION_ID, USER_ID))
                .thenThrow(new ResourceNotFoundException("Notification not found with id: " + NOTIFICATION_ID));

        // When / Then
        mockMvc.perform(put("/notifications/{notificationId}/read", NOTIFICATION_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(notificationService).markAsRead(NOTIFICATION_ID, USER_ID);
    }
}
