package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.engineer.entity.Engineer;
import com.sochi.fieldservice.incident.entity.Incident;
import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;

import java.time.LocalDateTime;

public class IncidentResponse {

    private Long id;
    private String title;
    private String description;
    private IncidentPriority priority;
    private IncidentStatus status;
    private String location;
    private Long assignedEngineerId;
    private String assignedEngineerName;
    private LocalDateTime slaDeadline;
    private boolean slaBreached;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private LocalDateTime closedAt;

    public IncidentResponse(Incident incident) {
        this.id = incident.getId();
        this.title = incident.getTitle();
        this.description = incident.getDescription();
        this.priority = incident.getPriority();
        this.status = incident.getStatus();
        this.location = incident.getLocation();
        this.slaDeadline = incident.getSlaDeadline();
        this.slaBreached = incident.isSlaBreached();
        this.createdAt = incident.getCreatedAt();
        this.updatedAt = incident.getUpdatedAt();
        this.resolvedAt = incident.getResolvedAt();
        this.closedAt = incident.getClosedAt();

        Engineer engineer = incident.getAssignedEngineer();

        if (engineer != null) {
            this.assignedEngineerId = engineer.getId();
            this.assignedEngineerName = engineer.getName();
        }
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }

    public Long getAssignedEngineerId() {
        return assignedEngineerId;
    }

    public String getAssignedEngineerName() {
        return assignedEngineerName;
    }

    public LocalDateTime getSlaDeadline() {
        return slaDeadline;
    }

    public boolean isSlaBreached() {
        return slaBreached;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }
}
