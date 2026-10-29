package com.sochi.fieldservice.report.dto;

import com.sochi.fieldservice.incident.entity.IncidentPriority;

import java.util.List;
import java.util.Map;

public class ReportSummaryResponse {

    private Double mttrHours;
    private Double slaCompliancePercentage;
    private Long totalIncidents;
    private Long resolvedIncidents;
    private Map<IncidentPriority, Long> incidentsByPriority;
    private List<EngineerPerformanceResponse> engineerPerformance;
    private List<DailyIncidentSeriesResponse> dailyCreatedVsResolved;

    public ReportSummaryResponse() {
    }

    public ReportSummaryResponse(
            Double mttrHours,
            Double slaCompliancePercentage,
            Long totalIncidents,
            Long resolvedIncidents,
            Map<IncidentPriority, Long> incidentsByPriority,
            List<EngineerPerformanceResponse> engineerPerformance,
            List<DailyIncidentSeriesResponse> dailyCreatedVsResolved) {
        this.mttrHours = mttrHours;
        this.slaCompliancePercentage = slaCompliancePercentage;
        this.totalIncidents = totalIncidents;
        this.resolvedIncidents = resolvedIncidents;
        this.incidentsByPriority = incidentsByPriority;
        this.engineerPerformance = engineerPerformance;
        this.dailyCreatedVsResolved = dailyCreatedVsResolved;
    }

    public Double getMttrHours() {
        return mttrHours;
    }

    public void setMttrHours(Double mttrHours) {
        this.mttrHours = mttrHours;
    }

    public Double getSlaCompliancePercentage() {
        return slaCompliancePercentage;
    }

    public void setSlaCompliancePercentage(Double slaCompliancePercentage) {
        this.slaCompliancePercentage = slaCompliancePercentage;
    }

    public Long getTotalIncidents() {
        return totalIncidents;
    }

    public void setTotalIncidents(Long totalIncidents) {
        this.totalIncidents = totalIncidents;
    }

    public Long getResolvedIncidents() {
        return resolvedIncidents;
    }

    public void setResolvedIncidents(Long resolvedIncidents) {
        this.resolvedIncidents = resolvedIncidents;
    }

    public Map<IncidentPriority, Long> getIncidentsByPriority() {
        return incidentsByPriority;
    }

    public void setIncidentsByPriority(Map<IncidentPriority, Long> incidentsByPriority) {
        this.incidentsByPriority = incidentsByPriority;
    }

    public List<EngineerPerformanceResponse> getEngineerPerformance() {
        return engineerPerformance;
    }

    public void setEngineerPerformance(List<EngineerPerformanceResponse> engineerPerformance) {
        this.engineerPerformance = engineerPerformance;
    }

    public List<DailyIncidentSeriesResponse> getDailyCreatedVsResolved() {
        return dailyCreatedVsResolved;
    }

    public void setDailyCreatedVsResolved(List<DailyIncidentSeriesResponse> dailyCreatedVsResolved) {
        this.dailyCreatedVsResolved = dailyCreatedVsResolved;
    }
}
