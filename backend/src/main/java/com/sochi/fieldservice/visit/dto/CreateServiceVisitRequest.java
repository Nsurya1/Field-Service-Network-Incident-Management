package com.sochi.fieldservice.visit.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CreateServiceVisitRequest {

    private Long engineerId;

    private LocalDate visitDate;

    private LocalDateTime checkInAt;

    private LocalDateTime checkOutAt;

    @NotBlank(message = "Work notes are required")
    @Size(min = 5, message = "Work notes must be at least 5 characters")
    private String workNotes;

    private String resolutionNotes;

    public CreateServiceVisitRequest() {
    }

    public CreateServiceVisitRequest(String workNotes) {
        this(null, null, null, null, workNotes, null);
    }

    public CreateServiceVisitRequest(Long engineerId, LocalDate visitDate, LocalDateTime checkInAt, LocalDateTime checkOutAt, String workNotes, String resolutionNotes) {
        this.engineerId = engineerId;
        this.visitDate = visitDate;
        this.checkInAt = checkInAt;
        this.checkOutAt = checkOutAt;
        this.workNotes = workNotes;
        this.resolutionNotes = resolutionNotes;
    }

    public Long getEngineerId() {
        return engineerId;
    }

    public void setEngineerId(Long engineerId) {
        this.engineerId = engineerId;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public LocalDateTime getCheckInAt() {
        return checkInAt;
    }

    public void setCheckInAt(LocalDateTime checkInAt) {
        this.checkInAt = checkInAt;
    }

    public LocalDateTime getCheckOutAt() {
        return checkOutAt;
    }

    public void setCheckOutAt(LocalDateTime checkOutAt) {
        this.checkOutAt = checkOutAt;
    }

    public String getWorkNotes() {
        return workNotes;
    }

    public void setWorkNotes(String workNotes) {
        this.workNotes = workNotes;
    }

    public String getResolutionNotes() {
        return resolutionNotes;
    }

    public void setResolutionNotes(String resolutionNotes) {
        this.resolutionNotes = resolutionNotes;
    }
}
