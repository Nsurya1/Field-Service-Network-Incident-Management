package com.sochi.fieldservice.notification.dto;

import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.notification.entity.Notification;
import com.sochi.fieldservice.notification.entity.NotificationType;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationDtoTest {

    @Test
    void testNotificationResponseFromEntity() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 101L);
        incident.setTitle("Core Router Outage");
        incident.setPriority(IncidentPriority.CRITICAL);

        LocalDateTime now = LocalDateTime.now();
        Notification notification = new Notification(incident, NotificationType.BREACHED, "SLA Breached", now);
        ReflectionTestUtils.setField(notification, "id", 1L);
        notification.setRead(true);

        NotificationResponse response = NotificationResponse.fromEntity(notification);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getIncidentId()).isEqualTo(101L);
        assertThat(response.getIncidentTitle()).isEqualTo("Core Router Outage");
        assertThat(response.getIncidentPriority()).isEqualTo(IncidentPriority.CRITICAL);
        assertThat(response.getType()).isEqualTo(NotificationType.BREACHED);
        assertThat(response.getMessage()).isEqualTo("SLA Breached");
        assertThat(response.isRead()).isTrue();
        assertThat(response.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void testNotificationResponseFromEntityWithNullIncident() {
        Notification notification = new Notification();
        ReflectionTestUtils.setField(notification, "id", 2L);
        notification.setType(NotificationType.WARNING);
        notification.setMessage("Warning threshold");

        NotificationResponse response = NotificationResponse.fromEntity(notification);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getIncidentId()).isNull();
        assertThat(response.getIncidentTitle()).isNull();
        assertThat(response.getIncidentPriority()).isNull();
        assertThat(response.getType()).isEqualTo(NotificationType.WARNING);
    }

    @Test
    void testUnreadCountResponse() {
        UnreadCountResponse response = new UnreadCountResponse();
        response.setUnreadCount(5L);
        assertThat(response.getUnreadCount()).isEqualTo(5L);

        UnreadCountResponse paramResponse = new UnreadCountResponse(12L);
        assertThat(paramResponse.getUnreadCount()).isEqualTo(12L);
    }
}
