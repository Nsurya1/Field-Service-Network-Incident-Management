package com.sochi.fieldservice.report.dto;

import java.time.LocalDate;

public class DailyIncidentSeriesResponse {

    private LocalDate date;
    private Long createdCount;
    private Long resolvedCount;

    public DailyIncidentSeriesResponse() {
    }

    public DailyIncidentSeriesResponse(LocalDate date, Long createdCount, Long resolvedCount) {
        this.date = date;
        this.createdCount = createdCount;
        this.resolvedCount = resolvedCount;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getCreatedCount() {
        return createdCount;
    }

    public void setCreatedCount(Long createdCount) {
        this.createdCount = createdCount;
    }

    public Long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(Long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }
}
