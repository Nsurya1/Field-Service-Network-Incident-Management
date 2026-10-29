package com.sochi.fieldservice.report.dto;

public class EngineerPerformanceResponse {

    private Long engineerId;
    private String engineerName;
    private Long resolvedCount;
    private Double avgResolutionTimeHours;

    public EngineerPerformanceResponse() {
    }

    public EngineerPerformanceResponse(Long engineerId, String engineerName, Long resolvedCount, Double avgResolutionTimeHours) {
        this.engineerId = engineerId;
        this.engineerName = engineerName;
        this.resolvedCount = resolvedCount;
        this.avgResolutionTimeHours = avgResolutionTimeHours;
    }

    public Long getEngineerId() {
        return engineerId;
    }

    public void setEngineerId(Long engineerId) {
        this.engineerId = engineerId;
    }

    public String getEngineerName() {
        return engineerName;
    }

    public void setEngineerName(String engineerName) {
        this.engineerName = engineerName;
    }

    public Long getResolvedCount() {
        return resolvedCount;
    }

    public void setResolvedCount(Long resolvedCount) {
        this.resolvedCount = resolvedCount;
    }

    public Double getAvgResolutionTimeHours() {
        return avgResolutionTimeHours;
    }

    public void setAvgResolutionTimeHours(Double avgResolutionTimeHours) {
        this.avgResolutionTimeHours = avgResolutionTimeHours;
    }
}
