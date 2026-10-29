package com.sochi.fieldservice.notification.service;

import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentStatus;
import com.sochi.fieldservice.incident.repository.IncidentRepository;
import com.sochi.fieldservice.notification.entity.Notification;
import com.sochi.fieldservice.notification.entity.NotificationType;
import com.sochi.fieldservice.notification.repository.NotificationRepository;
import com.sochi.fieldservice.observability.BusinessMetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class SlaAlertService {

    private static final Logger log = LoggerFactory.getLogger(SlaAlertService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final IncidentRepository incidentRepository;
    private final NotificationRepository notificationRepository;
    private final Clock clock;
    private final BusinessMetricsService businessMetricsService;

    public SlaAlertService(IncidentRepository incidentRepository,
                           NotificationRepository notificationRepository,
                           Clock clock,
                           BusinessMetricsService businessMetricsService) {
        this.incidentRepository = incidentRepository;
        this.notificationRepository = notificationRepository;
        this.clock = clock;
        this.businessMetricsService = businessMetricsService;
    }

    @Transactional
    public List<Notification> checkAndGenerateAlerts() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<IncidentStatus> activeStatuses = List.of(
                IncidentStatus.OPEN,
                IncidentStatus.ASSIGNED,
                IncidentStatus.IN_PROGRESS
        );

        List<Incident> activeIncidents = incidentRepository.findActiveIncidentsWithEngineer(activeStatuses);
        List<Notification> createdNotifications = new ArrayList<>();

        for (Incident incident : activeIncidents) {
            LocalDateTime createdAt = incident.getCreatedAt();
            LocalDateTime slaDeadline = incident.getSlaDeadline();

            if (createdAt == null || slaDeadline == null) {
                continue;
            }

            if (now.isAfter(slaDeadline) || now.isEqual(slaDeadline)) {
                if (!notificationRepository.existsByIncidentIdAndType(incident.getId(), NotificationType.BREACHED)) {
                    String message = String.format("Incident #%d '%s' [%s] has BREACHED its SLA deadline (%s).",
                            incident.getId(), incident.getTitle(), incident.getPriority(), slaDeadline.format(DATE_TIME_FORMATTER));
                    Notification notification = new Notification(incident, NotificationType.BREACHED, message, now);
                    Notification saved = notificationRepository.save(notification);
                    createdNotifications.add(saved);
                    if (businessMetricsService != null) {
                        businessMetricsService.recordSlaAlertGenerated(NotificationType.BREACHED);
                    }
                    log.warn("Generated SLA BREACHED alert for incident #{}", incident.getId());
                }
            } else {
                long totalSeconds = ChronoUnit.SECONDS.between(createdAt, slaDeadline);
                if (totalSeconds > 0) {
                    LocalDateTime warningThreshold = createdAt.plusSeconds((long) (totalSeconds * 0.8));
                    if (now.isAfter(warningThreshold) || now.isEqual(warningThreshold)) {
                        if (!notificationRepository.existsByIncidentIdAndType(incident.getId(), NotificationType.WARNING)) {
                            String message = String.format("Incident #%d '%s' [%s] has reached 80%% of SLA window. Deadline: %s.",
                                    incident.getId(), incident.getTitle(), incident.getPriority(), slaDeadline.format(DATE_TIME_FORMATTER));
                            Notification notification = new Notification(incident, NotificationType.WARNING, message, now);
                            Notification saved = notificationRepository.save(notification);
                            createdNotifications.add(saved);
                            if (businessMetricsService != null) {
                                businessMetricsService.recordSlaAlertGenerated(NotificationType.WARNING);
                            }
                            log.info("Generated SLA WARNING alert for incident #{}", incident.getId());
                        }
                    }
                }
            }
        }

        return createdNotifications;
    }
}
