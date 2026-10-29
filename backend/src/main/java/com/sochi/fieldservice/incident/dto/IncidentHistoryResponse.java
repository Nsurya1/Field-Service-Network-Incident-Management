package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.IncidentHistory;

import java.time.LocalDateTime;

public class IncidentHistoryResponse {
    private Long id;
    private Long incidentId;
    private String action;
    private String description;
    private String performedBy;
    private LocalDateTime timestamp;

    public IncidentHistoryResponse() {
    }

    public IncidentHistoryResponse(Long id, Long incidentId, String action, String description, String performedBy, LocalDateTime timestamp) {
        this.id = id;
        this.incidentId = incidentId;
        this.action = action;
        this.description = description;
        this.performedBy = performedBy != null ? performedBy : "system";
        this.timestamp = timestamp;
    }

    public IncidentHistoryResponse(IncidentHistory history) {
        this.id = history.getId();
        if (history.getIncident() != null) {
            this.incidentId = history.getIncident().getId();
        }
        this.action = history.getAction();
        this.description = history.getDescription();
        this.performedBy = history.getPerformedBy() != null ? history.getPerformedBy() : "system";
        this.timestamp = history.getTimestamp();
    }

    public Long getId() {
        return id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public String getAction() {
        return action;
    }

    public String getDescription() {
        return description;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
