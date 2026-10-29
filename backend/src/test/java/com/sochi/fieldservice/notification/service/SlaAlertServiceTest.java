package com.sochi.fieldservice.notification.service;

import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.notification.entity.Notification;
import com.sochi.fieldservice.notification.entity.NotificationType;
import com.sochi.fieldservice.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlaAlertServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private com.sochi.fieldservice.observability.BusinessMetricsService businessMetricsService;

    private Clock fixedClock;
    private SlaAlertService slaAlertService;

    private final LocalDateTime fixedNow = LocalDateTime.of(2026, 10, 29, 12, 0, 0);

    @BeforeEach
    void setUp() {
        Instant fixedInstant = fixedNow.atZone(ZoneId.systemDefault()).toInstant();
        fixedClock = Clock.fixed(fixedInstant, ZoneId.systemDefault());
        slaAlertService = new SlaAlertService(incidentRepository, notificationRepository, fixedClock, businessMetricsService);
    }

    @Test
    void shouldGenerateBreachedAlertWhenNowIsAtOrPastSlaDeadline() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 100L);
        incident.setTitle("Core Link Down");
        incident.setPriority(IncidentPriority.CRITICAL);
        incident.setCreatedAt(fixedNow.minusHours(4));
        incident.setSlaDeadline(fixedNow.minusMinutes(10)); // Past deadline

        when(incidentRepository.findActiveIncidentsWithEngineer(any()))
                .thenReturn(List.of(incident));
        when(notificationRepository.existsByIncidentIdAndType(100L, NotificationType.BREACHED))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<Notification> created = slaAlertService.checkAndGenerateAlerts();

        assertThat(created).hasSize(1);
        Notification notif = created.get(0);
        assertThat(notif.getType()).isEqualTo(NotificationType.BREACHED);
        assertThat(notif.getIncident()).isSameAs(incident);
        assertThat(notif.getMessage()).contains("BREACHED");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void shouldNotGenerateDuplicateBreachedAlertIfAlreadyExists() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 101L);
        incident.setTitle("Power Loss");
        incident.setPriority(IncidentPriority.HIGH);
        incident.setCreatedAt(fixedNow.minusHours(10));
        incident.setSlaDeadline(fixedNow.minusHours(1));

        when(incidentRepository.findActiveIncidentsWithEngineer(any()))
                .thenReturn(List.of(incident));
        when(notificationRepository.existsByIncidentIdAndType(101L, NotificationType.BREACHED))
                .thenReturn(true);

        List<Notification> created = slaAlertService.checkAndGenerateAlerts();

        assertThat(created).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldGenerateWarningAlertWhenAt80PercentOfSlaWindow() {
        // SLA window: 10 hours. 80% = 8 hours.
        // Created 9 hours ago, deadline is in 1 hour (total 10h). 9h elapsed >= 8h threshold.
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 102L);
        incident.setTitle("High Packet Drop");
        incident.setPriority(IncidentPriority.MEDIUM);
        incident.setCreatedAt(fixedNow.minusHours(9));
        incident.setSlaDeadline(fixedNow.plusHours(1));

        when(incidentRepository.findActiveIncidentsWithEngineer(any()))
                .thenReturn(List.of(incident));
        when(notificationRepository.existsByIncidentIdAndType(102L, NotificationType.WARNING))
                .thenReturn(false);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        List<Notification> created = slaAlertService.checkAndGenerateAlerts();

        assertThat(created).hasSize(1);
        Notification notif = created.get(0);
        assertThat(notif.getType()).isEqualTo(NotificationType.WARNING);
        assertThat(notif.getMessage()).contains("80% of SLA window");
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void shouldNotGenerateDuplicateWarningAlertIfAlreadyExists() {
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 103L);
        incident.setTitle("Latency Spike");
        incident.setPriority(IncidentPriority.LOW);
        incident.setCreatedAt(fixedNow.minusHours(60));
        incident.setSlaDeadline(fixedNow.plusHours(12));

        when(incidentRepository.findActiveIncidentsWithEngineer(any()))
                .thenReturn(List.of(incident));
        when(notificationRepository.existsByIncidentIdAndType(103L, NotificationType.WARNING))
                .thenReturn(true);

        List<Notification> created = slaAlertService.checkAndGenerateAlerts();

        assertThat(created).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldNotGenerateAlertWhenBelow80PercentWindow() {
        // Created 2 hours ago, deadline in 8 hours (total 10h). 2h < 8h (80%).
        Incident incident = new Incident();
        ReflectionTestUtils.setField(incident, "id", 104L);
        incident.setTitle("Minor Glitch");
        incident.setPriority(IncidentPriority.LOW);
        incident.setCreatedAt(fixedNow.minusHours(2));
        incident.setSlaDeadline(fixedNow.plusHours(8));

        when(incidentRepository.findActiveIncidentsWithEngineer(any()))
                .thenReturn(List.of(incident));

        List<Notification> created = slaAlertService.checkAndGenerateAlerts();

        assertThat(created).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void shouldSkipIncidentsWithNullDates() {
        Incident incident1 = new Incident();
        ReflectionTestUtils.setField(incident1, "id", 105L);
        incident1.setCreatedAt(null);
        incident1.setSlaDeadline(fixedNow.plusHours(1));

        Incident incident2 = new Incident();
        ReflectionTestUtils.setField(incident2, "id", 106L);
        incident2.setCreatedAt(fixedNow.minusHours(1));
        incident2.setSlaDeadline(null);

        when(incidentRepository.findActiveIncidentsWithEngineer(any()))
                .thenReturn(List.of(incident1, incident2));

        List<Notification> created = slaAlertService.checkAndGenerateAlerts();

        assertThat(created).isEmpty();
        verify(notificationRepository, never()).save(any(Notification.class));
    }
}
