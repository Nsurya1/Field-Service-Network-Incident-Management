package com.sochi.fieldservice.incident.dto;

import com.sochi.fieldservice.incident.entity.IncidentPriority;
import com.sochi.fieldservice.incident.entity.IncidentStatus;

import java.time.LocalDateTime;

public class IncidentFilterCriteria {

    private String search;
    private IncidentStatus status;
    private IncidentPriority priority;
    private Long assignedEngineerId;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private Boolean breachedOnly;

    public IncidentFilterCriteria() {
    }

    public IncidentFilterCriteria(
            String search,
            IncidentStatus status,
            IncidentPriority priority,
            Long assignedEngineerId,
            LocalDateTime createdFrom,
            LocalDateTime createdTo,
            Boolean breachedOnly) {
        this.search = search;
        this.status = status;
        this.priority = priority;
        this.assignedEngineerId = assignedEngineerId;
        this.createdFrom = createdFrom;
        this.createdTo = createdTo;
        this.breachedOnly = breachedOnly;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public IncidentStatus getStatus() {
        return status;
    }

    public void setStatus(IncidentStatus status) {
        this.status = status;
    }

    public IncidentPriority getPriority() {
        return priority;
    }

    public void setPriority(IncidentPriority priority) {
        this.priority = priority;
    }

    public Long getAssignedEngineerId() {
        return assignedEngineerId;
    }

    public void setAssignedEngineerId(Long assignedEngineerId) {
        this.assignedEngineerId = assignedEngineerId;
    }

    public LocalDateTime getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(LocalDateTime createdFrom) {
        this.createdFrom = createdFrom;
    }

    public LocalDateTime getCreatedTo() {
        return createdTo;
    }

    public void setCreatedTo(LocalDateTime createdTo) {
        this.createdTo = createdTo;
    }

    public Boolean getBreachedOnly() {
        return breachedOnly;
    }

    public void setBreachedOnly(Boolean breachedOnly) {
        this.breachedOnly = breachedOnly;
    }
}
