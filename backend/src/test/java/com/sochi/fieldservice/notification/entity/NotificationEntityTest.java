package com.sochi.fieldservice.notification.entity;

import com.sochi.fieldservice.incident.entity.Incident;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationEntityTest {

    @Test
    void testGettersAndSetters() {
        Incident incident = new Incident();
        incident.setTitle("Fibre Cut");

        LocalDateTime created = LocalDateTime.now().minusHours(1);
        Notification notification = new Notification();
        notification.setId(10L);
        notification.setIncident(incident);
        notification.setType(NotificationType.WARNING);
        notification.setMessage("80% SLA window reached");
        notification.setRead(true);
        notification.setCreatedAt(created);

        assertThat(notification.getId()).isEqualTo(10L);
        assertThat(notification.getIncident()).isSameAs(incident);
        assertThat(notification.getType()).isEqualTo(NotificationType.WARNING);
        assertThat(notification.getMessage()).isEqualTo("80% SLA window reached");
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getCreatedAt()).isEqualTo(created);
    }

    @Test
    void testConstructorWithDefaults() {
        Incident incident = new Incident();
        Notification notification = new Notification(incident, NotificationType.BREACHED, "Deadline missed");

        assertThat(notification.getIncident()).isSameAs(incident);
        assertThat(notification.getType()).isEqualTo(NotificationType.BREACHED);
        assertThat(notification.getMessage()).isEqualTo("Deadline missed");
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getCreatedAt()).isNotNull();
    }

    @Test
    void testPrePersist() {
        Notification notification = new Notification();
        assertThat(notification.getCreatedAt()).isNull();

        ReflectionTestUtils.invokeMethod(notification, "onCreate");
        assertThat(notification.getCreatedAt()).isNotNull();
    }
}
