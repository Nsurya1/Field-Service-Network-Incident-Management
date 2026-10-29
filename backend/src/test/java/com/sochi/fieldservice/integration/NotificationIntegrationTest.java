package com.sochi.fieldservice.integration;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.engineer.repository.EngineerRepository;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.notification.dto.NotificationResponse;
import com.sochi.fieldservice.notification.dto.UnreadCountResponse;
import com.sochi.fieldservice.notification.entity.Notification;
import com.sochi.fieldservice.notification.entity.NotificationType;
import com.sochi.fieldservice.notification.repository.NotificationRepository;
import com.sochi.fieldservice.notification.service.NotificationService;
import com.sochi.fieldservice.notification.service.SlaAlertService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SlaAlertService slaAlertService;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private EngineerRepository engineerRepository;

    @Test
    @DisplayName("Verify SLA alert generation, idempotency, read tracking, and bulk mark as read in database")
    void shouldGenerateAlertsAndManageReadStatus() {
        Engineer engineer = new Engineer();
        engineer.setName("Vijay Sla");
        engineer.setEmail("vijay.sla@fieldservice.com");
        engineer.setSpecialization("Core IP");
        Engineer savedEng = engineerRepository.save(engineer);

        LocalDateTime now = LocalDateTime.now();

        // 1. Breached incident (created 5h ago, 4h SLA deadline was 1h ago)
        Incident breachedIncident = new Incident();
        breachedIncident.setTitle("Core Switch Outage");
        breachedIncident.setDescription("Switch S1 offline");
        breachedIncident.setLocation("DC-North");
        breachedIncident.setPriority(IncidentPriority.CRITICAL);
        breachedIncident.setStatus(IncidentStatus.IN_PROGRESS);
        breachedIncident.setAssignedEngineer(savedEng);
        breachedIncident.setCreatedAt(now.minusHours(5));
        breachedIncident.setSlaDeadline(now.minusHours(1));
        breachedIncident = incidentRepository.save(breachedIncident);

        // 2. Warning incident (created 9h ago, deadline in 1h, total 10h window: 9h/10h = 90% >= 80%)
        Incident warningIncident = new Incident();
        warningIncident.setTitle("High CRC Errors");
        warningIncident.setDescription("Interface flapping");
        warningIncident.setLocation("Edge-West");
        warningIncident.setPriority(IncidentPriority.MEDIUM);
        warningIncident.setStatus(IncidentStatus.ASSIGNED);
        warningIncident.setAssignedEngineer(savedEng);
        warningIncident.setCreatedAt(now.minusHours(9));
        warningIncident.setSlaDeadline(now.plusHours(1));
        warningIncident = incidentRepository.save(warningIncident);

        // First run: should generate 1 BREACHED and 1 WARNING alert
        List<Notification> firstRun = slaAlertService.checkAndGenerateAlerts();
        assertThat(firstRun).hasSize(2);

        // Second run: should be completely idempotent (0 new alerts)
        List<Notification> secondRun = slaAlertService.checkAndGenerateAlerts();
        assertThat(secondRun).isEmpty();

        // Verify notification list and unread count
        List<NotificationResponse> all = notificationService.getNotifications();
        assertThat(all).hasSize(2);

        UnreadCountResponse unread = notificationService.getUnreadCount();
        assertThat(unread.getUnreadCount()).isEqualTo(2L);

        // Mark single notification as read
        NotificationResponse firstNotif = all.get(0);
        NotificationResponse readResponse = notificationService.markAsRead(firstNotif.getId());
        assertThat(readResponse.isRead()).isTrue();

        assertThat(notificationService.getUnreadCount().getUnreadCount()).isEqualTo(1L);

        // Mark all as read
        int marked = notificationService.markAllAsRead();
        assertThat(marked).isGreaterThanOrEqualTo(1);
        assertThat(notificationService.getUnreadCount().getUnreadCount()).isZero();
    }

    @Test
    @DisplayName("Verify unique constraint on (incident_id, type) at database level")
    void shouldEnforceDatabaseUniqueConstraintOnIncidentAndType() {
        LocalDateTime now = LocalDateTime.now();

        Incident incident = new Incident();
        incident.setTitle("Unique Constraint Test");
        incident.setDescription("Testing constraint");
        incident.setLocation("Lab-1");
        incident.setPriority(IncidentPriority.LOW);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setCreatedAt(now);
        incident.setSlaDeadline(now.plusHours(24));
        incident = incidentRepository.save(incident);

        Notification n1 = new Notification(incident, NotificationType.WARNING, "Warning 1", now);
        notificationRepository.saveAndFlush(n1);

        Notification n2 = new Notification(incident, NotificationType.WARNING, "Warning 2", now);
        assertThrows(DataIntegrityViolationException.class, () -> {
            notificationRepository.saveAndFlush(n2);
        });
    }
}
