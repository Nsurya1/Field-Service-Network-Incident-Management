package com.sochi.fieldservice.visit.dto;

import com.sochi.fieldservice.visit.entity.ServiceVisit;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ServiceVisitResponse {

    private Long id;
    private Long incidentId;
    private Long engineerId;
    private String engineerName;
    private LocalDate visitDate;
    private LocalDateTime checkInAt;
    private LocalDateTime checkOutAt;
    private String workNotes;
    private String resolutionNotes;
    private LocalDateTime createdAt;

    public ServiceVisitResponse() {
    }

    public ServiceVisitResponse(
            Long id,
            Long incidentId,
            Long engineerId,
            String engineerName,
            LocalDate visitDate,
            LocalDateTime checkInAt,
            LocalDateTime checkOutAt,
            String workNotes,
            String resolutionNotes,
            LocalDateTime createdAt) {
        this.id = id;
        this.incidentId = incidentId;
        this.engineerId = engineerId;
        this.engineerName = engineerName;
        this.visitDate = visitDate;
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.workNotes = workNotes;
        this.resolutionNotes = resolutionNotes;
        this.createdAt = createdAt;
    }

    public ServiceVisitResponse(ServiceVisit visit) {
        this.id = visit.getId();
        this.incidentId = visit.getIncident() != null ? visit.getIncident().getId() : null;
        if (visit.getEngineer() != null) {
            this.engineerId = visit.getEngineer().getId();
            this.engineerName = visit.getEngineer().getName();
        }
        this.visitDate = visit.getVisitDate();
        this.checkInAt = visit.getCheckInAt();
        this.checkOutAt = visit.getCheckOutAt();
        this.workNotes = visit.getWorkNotes();
        this.resolutionNotes = visit.getResolutionNotes();
        this.createdAt = visit.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public Long getEngineerId() {
        return engineerId;
    }

    public String getEngineerName() {
        return engineerName;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public LocalDateTime getCheckInAt() {
        return checkInAt;
    }

    public LocalDateTime getCheckOutAt() {
        return checkOutAt;
    }

    public String getWorkNotes() {
        return workNotes;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
