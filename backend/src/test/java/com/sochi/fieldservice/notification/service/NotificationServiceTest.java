package com.sochi.fieldservice.notification.service;

import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.notification.dto.NotificationResponse;
import com.sochi.fieldservice.notification.dto.UnreadCountResponse;
import com.sochi.fieldservice.notification.entity.Notification;
import com.sochi.fieldservice.notification.entity.NotificationType;
import com.sochi.fieldservice.notification.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void shouldReturnAllNotifications() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 1L);
        incident.setTitle("Fibre Cut");
        incident.setPriority(IncidentPriority.CRITICAL);

        Notification n1 = new Notification(incident, NotificationType.BREACHED, "Breached SLA");
        ReflectionTestUtils.setField(n1, "id", 10L);

        when(notificationRepository.findAllWithIncident()).thenReturn(List.of(n1));

        List<NotificationResponse> result = notificationService.getNotifications();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).getIncidentId()).isEqualTo(1L);
        assertThat(result.get(0).getMessage()).isEqualTo("Breached SLA");
    }

    @Test
    void shouldReturnUnreadCount() {
        when(notificationRepository.countByReadFalse()).thenReturn(7L);

        UnreadCountResponse result = notificationService.getUnreadCount();

        assertThat(result.getUnreadCount()).isEqualTo(7L);
    }

    @Test
    void shouldMarkNotificationAsReadWhenUnread() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 2L);

        Notification n = new Notification(incident, NotificationType.WARNING, "Warning");
        ReflectionTestUtils.setField(n, "id", 20L);
        n.setRead(false);

        when(notificationRepository.findByIdWithIncident(20L)).thenReturn(Optional.of(n));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(20L);

        assertThat(response.isRead()).isTrue();
        assertThat(n.isRead()).isTrue();
        verify(notificationRepository).save(n);
    }

    @Test
    void shouldNotReSaveWhenNotificationAlreadyRead() {
        Incident incident = new Incident();
        Notification n = new Notification(incident, NotificationType.WARNING, "Warning");
        ReflectionTestUtils.setField(n, "id", 21L);
        n.setRead(true);

        when(notificationRepository.findByIdWithIncident(21L)).thenReturn(Optional.of(n));

        NotificationResponse response = notificationService.markAsRead(21L);

        assertThat(response.isRead()).isTrue();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldThrowNotFoundWhenMarkingNonExistentNotification() {
        when(notificationRepository.findByIdWithIncident(999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
                notificationService.markAsRead(999L));

        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldMarkAllAsRead() {
        when(notificationRepository.markAllAsRead()).thenReturn(5);

        int updated = notificationService.markAllAsRead();

        assertThat(updated).isEqualTo(5);
        verify(notificationRepository).markAllAsRead();
    }
}
