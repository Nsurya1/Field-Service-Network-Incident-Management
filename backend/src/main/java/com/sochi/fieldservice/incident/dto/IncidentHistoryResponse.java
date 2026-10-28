package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.IncidentHistory;

import java.time.LocalDateTime;

public class IncidentHistoryResponse {
    private Long id;
    private Long incidentId;
    private String action;
    private String description;
    private LocalDateTime timestamp;

    public IncidentHistoryResponse(IncidentHistory history) {
        this.id = history.getId();
        if (history.getIncident() != null) {
            this.incidentId = history.getIncident().getId();
        }
        this.action = history.getAction();
        this.description = history.getDescription();
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
