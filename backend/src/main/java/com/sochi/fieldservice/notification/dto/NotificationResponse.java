package com.sochi.fieldservice.notification.dto;

import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.notification.entity.Notification;
import com.sochi.fieldservice.notification.entity.NotificationType;

import java.time.LocalDateTime;

public class NotificationResponse {
    private Long id;
    private Long incidentId;
    private String incidentTitle;
    private IncidentPriority incidentPriority;
    private NotificationType type;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;

    public NotificationResponse() {
    }

    public NotificationResponse(Long id, Long incidentId, String incidentTitle,
                                IncidentPriority incidentPriority, NotificationType type,
                                String message, boolean read, LocalDateTime createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.incidentTitle = incidentTitle;
        this.incidentPriority = incidentPriority;
        this.type = type;
        this.message = message;
        this.read = read;
        this.createdAt = createdAt;
    }

    public static NotificationResponse fromEntity(Notification notification) {
        Incident incident = notification.getIncident();
        return new NotificationResponse(
                notification.getId(),
                incident != null ? incident.getId() : null,
                incident != null ? incident.getTitle() : null,
                incident != null ? incident.getPriority() : null,
                notification.getType(),
                notification.getMessage(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }

    public Long getId() {
        return id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public String getIncidentTitle() {
        return incidentTitle;
    }

    public IncidentPriority getIncidentPriority() {
        return incidentPriority;
    }

    public NotificationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    public boolean isRead() {
        return read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
