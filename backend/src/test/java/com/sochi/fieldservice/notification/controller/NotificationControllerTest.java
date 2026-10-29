package com.sochi.fieldservice.notification.controller;

import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.notification.dto.NotificationResponse;
import com.sochi.fieldservice.notification.dto.UnreadCountResponse;
import com.sochi.fieldservice.notification.entity.NotificationType;
import com.sochi.fieldservice.notification.service.NotificationService;
import com.sochi.fieldservice.security.CustomUserDetailsService;
import com.sochi.fieldservice.security.SecurityConfig;
import com.sochi.fieldservice.security.jwt.JwtAccessDeniedHandler;
import com.sochi.fieldservice.security.jwt.JwtAuthenticationEntryPoint;
import com.sochi.fieldservice.security.jwt.JwtUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = NotificationController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = SecurityConfig.class
        )
)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private JwtUtils jwtUtils;

    @MockitoBean
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @MockitoBean
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Test
    void shouldReturnNotificationsList() throws Exception {
        NotificationResponse response = new NotificationResponse(
                1L, 10L, "Core Router Down", IncidentPriority.CRITICAL,
                NotificationType.BREACHED, "SLA Breached", false, LocalDateTime.now()
        );

        when(notificationService.getNotifications()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].incidentId").value(10L))
                .andExpect(jsonPath("$[0].type").value("BREACHED"))
                .andExpect(jsonPath("$[0].message").value("SLA Breached"))
                .andExpect(jsonPath("$[0].read").value(false));

        verify(notificationService).getNotifications();
    }

    @Test
    void shouldReturnUnreadCount() throws Exception {
        when(notificationService.getUnreadCount()).thenReturn(new UnreadCountResponse(4L));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(4L));

        verify(notificationService).getUnreadCount();
    }

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        NotificationResponse response = new NotificationResponse(
                5L, 20L, "High Latency", IncidentPriority.HIGH,
                NotificationType.WARNING, "80% reached", true, LocalDateTime.now()
        );

        when(notificationService.markAsRead(5L)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/notifications/5/read")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5L))
                .andExpect(jsonPath("$.read").value(true));

        verify(notificationService).markAsRead(5L);
    }

    @Test
    void shouldMarkAllNotificationsAsRead() throws Exception {
        when(notificationService.markAllAsRead()).thenReturn(3);

        mockMvc.perform(patch("/api/v1/notifications/read-all")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.markedAsRead").value(3));

        verify(notificationService).markAllAsRead();
    }
}
